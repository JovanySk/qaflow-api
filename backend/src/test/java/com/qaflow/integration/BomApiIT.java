package com.qaflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaflow.bom.infrastructure.BomRepository;
import com.qaflow.security.JwtProperties;
import com.qaflow.shared.web.CorrelationIdFilter;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BomApiIT {

    private static final String TEST_PASSWORD_HASH =
            "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.42"))
            .withDatabaseName("qaflow_bom_test")
            .withUsername("qaflow_test")
            .withPassword("qaflow_test_password")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoSpyBean
    private BomRepository bomRepository;

    private UserAccount admin;
    private String adminToken;
    private String operatorToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() {
        truncateDomainData();
        Instant createdAt = Instant.parse("2026-07-20T05:00:00Z");
        admin = userAccountRepository.save(UserAccount.create(
                "admin.demo", "Demo Administrator", TEST_PASSWORD_HASH, Role.ADMIN, true, createdAt, null));
        UserAccount operator = userAccountRepository.save(UserAccount.create(
                "operator.demo", "Demo Operator", TEST_PASSWORD_HASH, Role.OPERATOR, true, createdAt, null));
        UserAccount inspector = userAccountRepository.save(UserAccount.create(
                "inspector.demo", "Demo Inspector", TEST_PASSWORD_HASH,
                Role.QUALITY_INSPECTOR, true, createdAt, null));
        userAccountRepository.flush();
        adminToken = token(admin);
        operatorToken = token(operator);
        inspectorToken = token(inspector);
    }

    @Test
    void publishesFirstRevisionAndExposesCurrentHistoryExactQuantitiesAndAudit() {
        String productId = createProduct("FG-BOM-01", "Control panel", "bom-publish-product");
        String aluminumId = createMaterial("MAT-AL-01", "Aluminum", "KILOGRAM", "bom-publish-mat-1");
        String screwId = createMaterial("MAT-SC-01", "Screw", "PIECE", "bom-publish-mat-2");

        ResponseEntity<JsonNode> published = publish(
                productId,
                List.of(item(aluminumId, "2.5"), item(screwId, "4.000000")),
                adminToken,
                "bom-publish-001");

        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(published.getHeaders().getLocation())
                .hasToString("/api/v1/products/" + productId + "/boms/1");
        assertThat(published.getBody().path("revisionNumber").asInt()).isEqualTo(1);
        assertThat(published.getBody().path("current").asBoolean()).isTrue();
        assertThat(published.getBody().path("items").path(0).path("quantityPerUnit").asText())
                .isEqualTo("2.500000");
        assertThat(published.getBody().path("createdBy").path("username").asText())
                .isEqualTo("admin.demo");

        ResponseEntity<JsonNode> current = exchange(
                "/api/v1/products/" + productId + "/boms/current",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-current-001");
        assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(current.getBody().path("id").asText()).isEqualTo(published.getBody().path("id").asText());

        ResponseEntity<JsonNode> history = exchange(
                "/api/v1/products/" + productId + "/boms?sort=revisionNumber,desc",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-history-001");
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody().path("content")).hasSize(1);
        assertThat(history.getBody().path("content").path(0).path("itemCount").asLong()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? "
                        + "AND action = 'BOM_PUBLISHED' AND outcome = 'SUCCESS'",
                Integer.class,
                "bom-publish-001")).isEqualTo(1);
    }

    @Test
    void rejectsEmptyDuplicateAndInvalidQuantitiesWithoutPartialRevision() {
        String productId = createProduct("FG-BOM-02", "Validated product", "bom-valid-product");
        String materialId = createMaterial("MAT-VA-01", "Validated material", "PIECE", "bom-valid-material");

        ResponseEntity<JsonNode> empty = publish(productId, List.of(), adminToken, "bom-valid-001");
        assertError(empty, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "bom-valid-001");
        ResponseEntity<JsonNode> duplicate = publish(
                productId,
                List.of(item(materialId, "1"), item(materialId, "2")),
                adminToken,
                "bom-valid-002");
        assertError(duplicate, HttpStatus.BAD_REQUEST, "DUPLICATE_BOM_MATERIAL", "bom-valid-002");

        for (Map.Entry<String, String> invalid : Map.of(
                "bom-valid-003", "0",
                "bom-valid-004", "-1",
                "bom-valid-005", "1.0000001").entrySet()) {
            ResponseEntity<JsonNode> response = publish(
                    productId,
                    List.of(item(materialId, invalid.getValue())),
                    adminToken,
                    invalid.getKey());
            assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", invalid.getKey());
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM boms", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bom_items", Integer.class)).isZero();
    }

    @Test
    void rejectsMissingOrInactiveProductAndMaterialWithStableErrors() {
        String productId = createProduct("FG-BOM-03", "Reference product", "bom-ref-product");
        String materialId = createMaterial("MAT-REF-01", "Reference material", "PIECE", "bom-ref-material");

        ResponseEntity<JsonNode> missingCurrent = exchange(
                "/api/v1/products/" + productId + "/boms/current",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-ref-current-001");
        assertError(
                missingCurrent,
                HttpStatus.NOT_FOUND,
                "CURRENT_BOM_NOT_FOUND",
                "bom-ref-current-001");
        ResponseEntity<JsonNode> missingRevision = exchange(
                "/api/v1/products/" + productId + "/boms/1",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-ref-revision-001");
        assertError(
                missingRevision,
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "bom-ref-revision-001");

        ResponseEntity<JsonNode> missingProduct = publish(
                "999999",
                List.of(item(materialId, "1")),
                adminToken,
                "bom-ref-001");
        assertError(missingProduct, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "bom-ref-001");

        ResponseEntity<JsonNode> missingMaterial = publish(
                productId,
                List.of(item("999999", "1")),
                adminToken,
                "bom-ref-002");
        assertError(missingMaterial, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "bom-ref-002");

        ResponseEntity<JsonNode> materialDeactivation = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "0", "active", false),
                adminToken,
                "bom-ref-setup-1");
        assertThat(materialDeactivation.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> inactiveMaterial = publish(
                productId,
                List.of(item(materialId, "1")),
                adminToken,
                "bom-ref-003");
        assertError(inactiveMaterial, HttpStatus.CONFLICT, "RESOURCE_INACTIVE", "bom-ref-003");

        String activeMaterialId = createMaterial(
                "MAT-REF-02", "Active material", "PIECE", "bom-ref-material-2");
        ResponseEntity<JsonNode> productDeactivation = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                Map.of("version", "0", "active", false),
                adminToken,
                "bom-ref-setup-2");
        assertThat(productDeactivation.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> inactiveProduct = publish(
                productId,
                List.of(item(activeMaterialId, "1")),
                adminToken,
                "bom-ref-004");
        assertError(inactiveProduct, HttpStatus.CONFLICT, "RESOURCE_INACTIVE", "bom-ref-004");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM boms", Integer.class)).isZero();
    }

    @Test
    void replacementSupersedesHistoryKeepsOneCurrentAndDatabasePreventsItemMutation() {
        String productId = createProduct("FG-BOM-04", "Revision product", "bom-revision-product");
        String materialId = createMaterial("MAT-RV-01", "Revision material", "PIECE", "bom-revision-material");
        ResponseEntity<JsonNode> first = publish(
                productId,
                List.of(item(materialId, "1")),
                adminToken,
                "bom-revision-001");
        ResponseEntity<JsonNode> immutableUnit = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "0", "unitOfMeasure", "KILOGRAM"),
                adminToken,
                "bom-revision-unit-001");
        assertError(
                immutableUnit,
                HttpStatus.CONFLICT,
                "MATERIAL_UNIT_IMMUTABLE",
                "bom-revision-unit-001");
        ResponseEntity<JsonNode> second = publish(
                productId,
                List.of(item(materialId, "2.250000")),
                adminToken,
                "bom-revision-002");

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().path("revisionNumber").asInt()).isEqualTo(2);
        ResponseEntity<JsonNode> firstHistory = exchange(
                "/api/v1/products/" + productId + "/boms/1",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-revision-003");
        assertThat(firstHistory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstHistory.getBody().path("current").asBoolean()).isFalse();
        assertThat(firstHistory.getBody().path("supersededAt").isTextual()).isTrue();
        assertThat(firstHistory.getBody().path("supersededBy").path("username").asText())
                .isEqualTo("admin.demo");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM boms WHERE product_id = ? AND is_current = 1",
                Integer.class,
                Long.parseLong(productId))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM boms WHERE product_id = ?",
                Integer.class,
                Long.parseLong(productId))).isEqualTo(2);

        ResponseEntity<JsonNode> productDeactivation = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                Map.of("version", "0", "active", false),
                adminToken,
                "bom-revision-deactivate-001");
        assertThat(productDeactivation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange(
                "/api/v1/products/" + productId + "/boms/current",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-revision-after-deactivate-001").getStatusCode()).isEqualTo(HttpStatus.OK);

        long firstBomId = first.getBody().path("id").asLong();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE bom_items SET quantity_per_unit = 9 WHERE bom_id = ?",
                firstBomId)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void enforcesAuthenticationAndRoleBoundaries() {
        String productId = createProduct("FG-BOM-05", "Permission product", "bom-perm-product");
        String materialId = createMaterial("MAT-PE-01", "Permission material", "PIECE", "bom-perm-material");
        assertThat(publish(
                productId,
                List.of(item(materialId, "1")),
                adminToken,
                "bom-perm-setup").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<JsonNode> anonymous = exchange(
                "/api/v1/products/" + productId + "/boms/current",
                HttpMethod.GET,
                null,
                null,
                "bom-perm-001");
        assertError(anonymous, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "bom-perm-001");
        ResponseEntity<JsonNode> inspector = exchange(
                "/api/v1/products/" + productId + "/boms",
                HttpMethod.GET,
                null,
                inspectorToken,
                "bom-perm-002");
        assertError(inspector, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "bom-perm-002");
        ResponseEntity<JsonNode> operatorWrite = publish(
                productId,
                List.of(item(materialId, "2")),
                operatorToken,
                "bom-perm-003");
        assertError(operatorWrite, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "bom-perm-003");
        assertThat(exchange(
                "/api/v1/products/" + productId + "/boms/current",
                HttpMethod.GET,
                null,
                operatorToken,
                "bom-perm-004").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void concurrentPublicationsAllowOneCreationAndReturnOneControlledConflict() throws Exception {
        String productId = createProduct("FG-BOM-06", "Concurrent product", "bom-race-product");
        String materialId = createMaterial("MAT-CO-01", "Concurrent material", "PIECE", "bom-race-material");
        long numericProductId = Long.parseLong(productId);
        CountDownLatch bothObserved = new CountDownLatch(2);
        doAnswer(invocation -> {
            Optional<Long> observed = jdbcTemplate.query(
                    "SELECT id FROM boms WHERE product_id = ? AND is_current = 1",
                    resultSet -> resultSet.next()
                            ? Optional.of(resultSet.getLong("id"))
                            : Optional.empty(),
                    numericProductId);
            bothObserved.countDown();
            if (!bothObserved.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent BOM publications did not observe together");
            }
            return observed;
        }).when(bomRepository).findCurrentId(numericProductId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<JsonNode>> first = executor.submit(() -> publish(
                    productId,
                    List.of(item(materialId, "1")),
                    adminToken,
                    "bom-race-001"));
            Future<ResponseEntity<JsonNode>> second = executor.submit(() -> publish(
                    productId,
                    List.of(item(materialId, "2")),
                    adminToken,
                    "bom-race-002"));
            ResponseEntity<JsonNode> firstResponse = first.get(30, TimeUnit.SECONDS);
            ResponseEntity<JsonNode> secondResponse = second.get(30, TimeUnit.SECONDS);

            assertThat(Set.of(firstResponse.getStatusCode(), secondResponse.getStatusCode()))
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
            ResponseEntity<JsonNode> conflict = firstResponse.getStatusCode() == HttpStatus.CONFLICT
                    ? firstResponse
                    : secondResponse;
            assertThat(conflict.getBody().path("code").asText()).isEqualTo("BOM_CONCURRENTLY_CHANGED");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM boms WHERE product_id = ? AND is_current = 1",
                    Integer.class,
                    numericProductId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM boms WHERE product_id = ?",
                    Integer.class,
                    numericProductId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void runtimeOpenApiPublishesAllFourBomOperations() {
        ResponseEntity<JsonNode> openApi = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode paths = openApi.getBody().path("paths");
        assertThat(paths.path("/api/v1/products/{productId}/boms").has("get")).isTrue();
        assertThat(paths.path("/api/v1/products/{productId}/boms").has("post")).isTrue();
        assertThat(paths.path("/api/v1/products/{productId}/boms/current").has("get")).isTrue();
        assertThat(paths.path("/api/v1/products/{productId}/boms/{revisionNumber}").has("get")).isTrue();
    }

    private String createProduct(String sku, String name, String correlationId) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", sku, "name", name),
                adminToken,
                correlationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("id").asText();
    }

    private String createMaterial(String code, String name, String unit, String correlationId) {
        ResponseEntity<JsonNode> response = exchange(
                "/api/v1/materials",
                HttpMethod.POST,
                Map.of("code", code, "name", name, "unitOfMeasure", unit),
                adminToken,
                correlationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("id").asText();
    }

    private Map<String, String> item(String materialId, String quantity) {
        return Map.of("materialId", materialId, "quantityPerUnit", quantity);
    }

    private ResponseEntity<JsonNode> publish(
            String productId,
            List<Map<String, String>> items,
            String bearerToken,
            String correlationId) {
        return exchange(
                "/api/v1/products/" + productId + "/boms",
                HttpMethod.POST,
                Map.of("items", items),
                bearerToken,
                correlationId);
    }

    private ResponseEntity<JsonNode> exchange(
            String path,
            HttpMethod method,
            Object body,
            String bearerToken,
            String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdFilter.HEADER_NAME, correlationId);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private void assertError(
            ResponseEntity<JsonNode> response,
            HttpStatus status,
            String code,
            String correlationId) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().path("correlationId").asText()).isEqualTo(correlationId);
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo(correlationId);
        assertThat(response.getBody().path("fieldErrors").isArray()).isTrue();
    }

    private String token(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .id(UUID.randomUUID().toString())
                .claim("role", user.role().name())
                .claim("authVersion", user.authVersion())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    private void truncateDomainData() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                statement.execute("TRUNCATE TABLE audit_events");
                statement.execute("TRUNCATE TABLE quality_inspections");
                statement.execute("TRUNCATE TABLE production_order_status_history");
                statement.execute("TRUNCATE TABLE inventory_movements");
                statement.execute("TRUNCATE TABLE production_order_materials");
                statement.execute("TRUNCATE TABLE production_orders");
                statement.execute("TRUNCATE TABLE inventory_balances");
                statement.execute("TRUNCATE TABLE bom_items");
                statement.execute("TRUNCATE TABLE boms");
                statement.execute("TRUNCATE TABLE materials");
                statement.execute("TRUNCATE TABLE products");
                statement.execute("TRUNCATE TABLE users");
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }
}
