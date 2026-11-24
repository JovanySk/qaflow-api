package com.qaflow.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaflow.security.JwtProperties;
import com.qaflow.product.infrastructure.ProductRepository;
import com.qaflow.shared.web.CorrelationIdFilter;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogApiIT {

    private static final String TEST_PASSWORD_HASH =
            "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.42"))
            .withDatabaseName("qaflow_catalog_test")
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
    private ProductRepository productRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private UserAccount admin;
    private UserAccount operator;
    private UserAccount inspector;
    private String adminToken;
    private String operatorToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() {
        truncateDomainData();
        Instant createdAt = Instant.parse("2026-07-20T04:00:00Z");
        admin = userAccountRepository.save(UserAccount.create(
                "admin.demo", "Demo Administrator", TEST_PASSWORD_HASH, Role.ADMIN, true, createdAt, null));
        operator = userAccountRepository.save(UserAccount.create(
                "operator.demo", "Demo Operator", TEST_PASSWORD_HASH, Role.OPERATOR, true, createdAt, null));
        inspector = userAccountRepository.save(UserAccount.create(
                "inspector.demo", "Demo Inspector", TEST_PASSWORD_HASH,
                Role.QUALITY_INSPECTOR, true, createdAt, null));
        userAccountRepository.flush();
        adminToken = token(admin);
        operatorToken = token(operator);
        inspectorToken = token(inspector);
    }

    @Test
    void productCrudSupportsNormalizedUniquenessSearchNullDescriptionAndLogicalDeactivation() {
        ResponseEntity<JsonNode> created = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of(
                        "sku", " FG-PANEL-01 ",
                        "name", "Finished Panel",
                        "description", "Initial description"),
                adminToken,
                "catalog-product-001");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String productId = created.getBody().path("id").asText();
        assertThat(created.getHeaders().getLocation()).hasToString("/api/v1/products/" + productId);
        assertThat(created.getBody().path("sku").asText()).isEqualTo("FG-PANEL-01");
        assertThat(created.getBody().path("version").asText()).isEqualTo("0");
        assertThat(created.getBody().path("active").asBoolean()).isTrue();
        assertThat(created.getBody().path("createdBy").path("username").asText()).isEqualTo("admin.demo");

        ResponseEntity<JsonNode> detail = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-product-detail-001");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("sku").asText()).isEqualTo("FG-PANEL-01");

        ResponseEntity<JsonNode> duplicate = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", "fg-panel-01", "name", "Duplicate"),
                adminToken,
                "catalog-product-002");
        assertError(duplicate, HttpStatus.CONFLICT, "DUPLICATE_SKU", "catalog-product-002");

        LinkedHashMap<String, Object> clearDescription = new LinkedHashMap<>();
        clearDescription.put("version", "0");
        clearDescription.put("name", "Finished Panel v2");
        clearDescription.put("description", null);
        ResponseEntity<JsonNode> updated = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                clearDescription,
                adminToken,
                "catalog-product-003");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("version").asText()).isEqualTo("1");
        assertThat(updated.getBody().path("description").isNull()).isTrue();

        ResponseEntity<JsonNode> deactivated = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                Map.of("version", "1", "active", false),
                adminToken,
                "catalog-product-004");
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody().path("version").asText()).isEqualTo("2");
        assertThat(deactivated.getBody().path("active").asBoolean()).isFalse();

        ResponseEntity<JsonNode> page = exchange(
                "/api/v1/products?search=panel&active=false&page=0&size=10&sort=sku,asc",
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-product-005");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().path("content").size()).isEqualTo(1);
        assertThat(page.getBody().path("content").path(0).path("id").asText()).isEqualTo(productId);
        assertThat(page.getBody().path("page").path("totalElements").asLong()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE resource_type = 'PRODUCT' AND outcome = 'SUCCESS'",
                Integer.class)).isEqualTo(3);
    }

    @Test
    void materialCreationIsAtomicWithZeroBalanceAndUnitMayChangeBeforeHistory() {
        ResponseEntity<JsonNode> created = createMaterial("MAT-AL-01", "Aluminum", "KILOGRAM", "catalog-material-001");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String materialId = created.getBody().path("id").asText();
        Map<String, Object> balance = jdbcTemplate.queryForMap(
                "SELECT on_hand_quantity, reserved_quantity, available_quantity, version "
                        + "FROM inventory_balances WHERE material_id = ?",
                Long.parseLong(materialId));
        assertThat((BigDecimal) balance.get("on_hand_quantity")).isEqualByComparingTo("0");
        assertThat((BigDecimal) balance.get("reserved_quantity")).isEqualByComparingTo("0");
        assertThat((BigDecimal) balance.get("available_quantity")).isEqualByComparingTo("0");
        assertThat(((Number) balance.get("version")).longValue()).isZero();

        ResponseEntity<JsonNode> duplicate = createMaterial(
                " mat-al-01 ", "Duplicate aluminum", "KILOGRAM", "catalog-material-002");
        assertError(
                duplicate,
                HttpStatus.CONFLICT,
                "DUPLICATE_MATERIAL_CODE",
                "catalog-material-002");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_balances", Integer.class))
                .isEqualTo(1);

        ResponseEntity<JsonNode> changedUnit = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "0", "unitOfMeasure", "GRAM"),
                adminToken,
                "catalog-material-003");
        assertThat(changedUnit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changedUnit.getBody().path("unitOfMeasure").asText()).isEqualTo("GRAM");
        assertThat(changedUnit.getBody().path("version").asText()).isEqualTo("1");

        ResponseEntity<JsonNode> detail = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-material-detail-001");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("code").asText()).isEqualTo("MAT-AL-01");

        ResponseEntity<JsonNode> deactivated = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "1", "active", false),
                adminToken,
                "catalog-material-005");
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody().path("active").asBoolean()).isFalse();
        assertThat(deactivated.getBody().path("version").asText()).isEqualTo("2");

        ResponseEntity<JsonNode> stale = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "1", "name", "Stale material"),
                adminToken,
                "catalog-material-006");
        assertError(stale, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "catalog-material-006");

        ResponseEntity<JsonNode> page = exchange(
                "/api/v1/materials?search=alum&unitOfMeasure=GRAM&active=false&sort=code,asc",
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-material-004");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().path("content").size()).isEqualTo(1);
        assertThat(page.getBody().path("content").path(0).path("id").asText()).isEqualTo(materialId);
    }

    @Test
    void materialUnitBecomesImmutableAfterInventoryHistory() {
        ResponseEntity<JsonNode> created = createMaterial("MAT-ST-01", "Steel", "KILOGRAM", "catalog-unit-001");
        String materialId = created.getBody().path("id").asText();
        jdbcTemplate.update(
                "UPDATE inventory_balances SET on_hand_quantity = 1 WHERE material_id = ?",
                Long.parseLong(materialId));
        jdbcTemplate.update(
                "INSERT INTO inventory_movements "
                        + "(material_id, movement_type, direction, quantity, on_hand_after, reserved_after, "
                        + "reference_code, performed_by, correlation_id) "
                        + "VALUES (?, 'ENTRY', 'IN', 1, 1, 0, 'history-setup', ?, 'catalog-unit-setup')",
                Long.parseLong(materialId),
                admin.id());

        ResponseEntity<JsonNode> conflict = exchange(
                "/api/v1/materials/" + materialId,
                HttpMethod.PATCH,
                Map.of("version", "0", "unitOfMeasure", "GRAM"),
                adminToken,
                "catalog-unit-002");

        assertError(conflict, HttpStatus.CONFLICT, "MATERIAL_UNIT_IMMUTABLE", "catalog-unit-002");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT unit_of_measure FROM materials WHERE id = ?",
                String.class,
                Long.parseLong(materialId))).isEqualTo("KILOGRAM");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? AND outcome = 'REJECTED'",
                Integer.class,
                "catalog-unit-002")).isEqualTo(1);
    }

    @Test
    void catalogPermissionsReturnExpected403And401Boundaries() {
        ResponseEntity<JsonNode> anonymous = exchange(
                "/api/v1/products", HttpMethod.GET, null, null, "catalog-permission-001");
        assertError(anonymous, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "catalog-permission-001");

        ResponseEntity<JsonNode> inspectorProducts = exchange(
                "/api/v1/products", HttpMethod.GET, null, inspectorToken, "catalog-permission-002");
        assertError(inspectorProducts, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "catalog-permission-002");
        ResponseEntity<JsonNode> inspectorMaterials = exchange(
                "/api/v1/materials", HttpMethod.GET, null, inspectorToken, "catalog-permission-003");
        assertError(inspectorMaterials, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "catalog-permission-003");

        ResponseEntity<JsonNode> operatorProductCreate = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", "NO-AUTH", "name", "Forbidden"),
                operatorToken,
                "catalog-permission-004");
        assertError(operatorProductCreate, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "catalog-permission-004");
        ResponseEntity<JsonNode> operatorMaterialCreate = exchange(
                "/api/v1/materials",
                HttpMethod.POST,
                Map.of("code", "NO-AUTH", "name", "Forbidden", "unitOfMeasure", "PIECE"),
                operatorToken,
                "catalog-permission-005");
        assertError(operatorMaterialCreate, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "catalog-permission-005");

        assertThat(exchange(
                "/api/v1/products", HttpMethod.GET, null, operatorToken, "catalog-permission-006")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange(
                "/api/v1/materials", HttpMethod.GET, null, operatorToken, "catalog-permission-007")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidUnknownMissingAndStaleCatalogRequestsUseStableErrors() {
        ResponseEntity<JsonNode> invalidProduct = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", " ", "name", ""),
                adminToken,
                "catalog-validation-001");
        assertError(invalidProduct, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "catalog-validation-001");
        assertThat(invalidProduct.getBody().path("fieldErrors").size()).isGreaterThanOrEqualTo(2);

        ResponseEntity<JsonNode> unknownField = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", "FG-VALID", "name", "Valid", "cost", 99),
                adminToken,
                "catalog-validation-002");
        assertError(unknownField, HttpStatus.BAD_REQUEST, "UNKNOWN_FIELD", "catalog-validation-002");

        ResponseEntity<JsonNode> invalidUnit = exchange(
                "/api/v1/materials",
                HttpMethod.POST,
                Map.of("code", "MAT-BAD", "name", "Bad", "unitOfMeasure", "TON"),
                adminToken,
                "catalog-validation-003");
        assertError(invalidUnit, HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "catalog-validation-003");

        ResponseEntity<JsonNode> missing = exchange(
                "/api/v1/products/999999",
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-validation-004");
        assertError(missing, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "catalog-validation-004");

        ResponseEntity<JsonNode> noChanges = exchange(
                "/api/v1/products/999999",
                HttpMethod.PATCH,
                Map.of("version", "0"),
                adminToken,
                "catalog-validation-no-change-001");
        assertError(
                noChanges,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "catalog-validation-no-change-001");

        ResponseEntity<JsonNode> invalidSort = exchange(
                "/api/v1/materials?sort=unsupported,asc",
                HttpMethod.GET,
                null,
                operatorToken,
                "catalog-validation-005");
        assertError(invalidSort, HttpStatus.BAD_REQUEST, "INVALID_QUERY_PARAMETER", "catalog-validation-005");

        ResponseEntity<JsonNode> product = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", "FG-VERSION", "name", "Versioned"),
                adminToken,
                "catalog-validation-006");
        String productId = product.getBody().path("id").asText();
        assertThat(exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                Map.of("version", "0", "name", "First update"),
                adminToken,
                "catalog-validation-007").getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> stale = exchange(
                "/api/v1/products/" + productId,
                HttpMethod.PATCH,
                Map.of("version", "0", "name", "Stale update"),
                adminToken,
                "catalog-validation-008");
        assertError(stale, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "catalog-validation-008");
    }

    @Test
    void runtimeOpenApiPublishesAllEightCatalogOperations() {
        ResponseEntity<JsonNode> openApi = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode paths = openApi.getBody().path("paths");
        assertThat(paths.path("/api/v1/products").has("get")).isTrue();
        assertThat(paths.path("/api/v1/products").has("post")).isTrue();
        assertThat(paths.path("/api/v1/products/{productId}").has("get")).isTrue();
        assertThat(paths.path("/api/v1/products/{productId}").has("patch")).isTrue();
        assertThat(paths.path("/api/v1/materials").has("get")).isTrue();
        assertThat(paths.path("/api/v1/materials").has("post")).isTrue();
        assertThat(paths.path("/api/v1/materials/{materialId}").has("get")).isTrue();
        assertThat(paths.path("/api/v1/materials/{materialId}").has("patch")).isTrue();
    }

    @Test
    void concurrentProductWritersAllowExactlyOneCommitForTheSameVersion() throws Exception {
        ResponseEntity<JsonNode> created = exchange(
                "/api/v1/products",
                HttpMethod.POST,
                Map.of("sku", "FG-RACE", "name", "Concurrent product"),
                adminToken,
                "catalog-race-001");
        long productId = created.getBody().path("id").asLong();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch releaseWriters = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> concurrentUpdate(
                    productId, "Writer one", bothLoaded, releaseWriters));
            Future<Boolean> second = executor.submit(() -> concurrentUpdate(
                    productId, "Writer two", bothLoaded, releaseWriters));
            assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
            releaseWriters.countDown();

            int commits = 0;
            int optimisticConflicts = 0;
            for (Future<Boolean> result : List.of(first, second)) {
                try {
                    if (result.get(20, TimeUnit.SECONDS)) {
                        commits++;
                    }
                } catch (ExecutionException exception) {
                    if (hasCause(exception, OptimisticLockingFailureException.class)) {
                        optimisticConflicts++;
                    } else {
                        throw exception;
                    }
                }
            }
            assertThat(commits).isEqualTo(1);
            assertThat(optimisticConflicts).isEqualTo(1);
            assertThat(productRepository.findById(productId).orElseThrow().version()).isEqualTo(1);
        } finally {
            releaseWriters.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ResponseEntity<JsonNode> createMaterial(
            String code,
            String name,
            String unitOfMeasure,
            String correlationId) {
        return exchange(
                "/api/v1/materials",
                HttpMethod.POST,
                Map.of("code", code, "name", name, "unitOfMeasure", unitOfMeasure),
                adminToken,
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
        if (status == HttpStatus.UNAUTHORIZED) {
            assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        }
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

    private boolean concurrentUpdate(
            long productId,
            String name,
            CountDownLatch bothLoaded,
            CountDownLatch releaseWriters) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Boolean result = transaction.execute(status -> {
            com.qaflow.product.domain.Product product = productRepository.findById(productId).orElseThrow();
            bothLoaded.countDown();
            try {
                if (!releaseWriters.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent writers were not released in time");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Concurrent writer was interrupted", exception);
            }
            product.changeName(name);
            product.markUpdated(Instant.now(), admin.id());
            productRepository.saveAndFlush(product);
            return true;
        });
        return Boolean.TRUE.equals(result);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expected) {
        Throwable current = throwable;
        while (current != null) {
            if (expected.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
