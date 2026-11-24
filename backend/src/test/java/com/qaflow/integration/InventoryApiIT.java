package com.qaflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryApiIT {

    private static final String TEST_PASSWORD_HASH =
            "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.42"))
            .withDatabaseName("qaflow_inventory_test")
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

    private UserAccount admin;
    private UserAccount operator;
    private String adminToken;
    private String operatorToken;
    private String inspectorToken;

    @BeforeEach
    void setUp() {
        truncateDomainData();
        Instant createdAt = Instant.parse("2026-07-20T08:00:00Z");
        admin = userAccountRepository.save(UserAccount.create(
                "admin.demo", "Demo Administrator", TEST_PASSWORD_HASH, Role.ADMIN, true, createdAt, null));
        operator = userAccountRepository.save(UserAccount.create(
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
    void entryAndExactOutputUpdateBalanceLedgerAndAuditAtomically() {
        String materialId = createMaterial("MAT-INV-01", "Steel", "KILOGRAM", "inventory-material-001");

        ResponseEntity<JsonNode> entry = movement(
                materialId, "ENTRY", "100", "PO-RECEIPT-001", null, operatorToken, "inventory-entry-001");
        assertThat(entry.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entry.getHeaders().getLocation())
                .hasToString("/api/v1/inventory/movements/" + entry.getBody().path("id").asText());
        assertThat(entry.getBody().path("direction").asText()).isEqualTo("IN");
        assertThat(entry.getBody().path("quantity").asText()).isEqualTo("100.000000");
        assertThat(entry.getBody().path("onHandAfter").asText()).isEqualTo("100.000000");

        ResponseEntity<JsonNode> output = movement(
                materialId, "MANUAL_OUT", "100", null, "Material de muestra",
                operatorToken, "inventory-out-001");
        assertThat(output.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(output.getBody().path("direction").asText()).isEqualTo("OUT");
        assertThat(output.getBody().path("onHandAfter").asText()).isEqualTo("0.000000");

        ResponseEntity<JsonNode> balance = exchange(
                "/api/v1/inventory/balances/" + materialId,
                HttpMethod.GET, null, adminToken, "inventory-balance-001");
        assertThat(balance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(balance.getBody().path("onHandQuantity").asText()).isEqualTo("0.000000");
        assertThat(balance.getBody().path("reservedQuantity").asText()).isEqualTo("0.000000");
        assertThat(balance.getBody().path("availableQuantity").asText()).isEqualTo("0.000000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE material_id = ?",
                Integer.class, Long.parseLong(materialId))).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action = 'INVENTORY_MOVEMENT_CREATED' "
                        + "AND outcome = 'SUCCESS'",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void insufficientStockReturnsConflictAndRollsBackBalanceMovementAndSuccessAudit() {
        String materialId = createMaterial("MAT-INV-02", "Copper", "KILOGRAM", "inventory-material-002");
        movement(materialId, "ENTRY", "24.999999", "RECEIPT-002", null,
                operatorToken, "inventory-stock-setup");

        ResponseEntity<JsonNode> rejected = movement(
                materialId, "MANUAL_OUT", "25", "ISSUE-002", null,
                operatorToken, "inventory-stock-rejected");

        assertError(rejected, HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "inventory-stock-rejected");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT on_hand_quantity FROM inventory_balances WHERE material_id = ?",
                String.class, Long.parseLong(materialId))).isEqualTo("24.999999");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE material_id = ?",
                Integer.class, Long.parseLong(materialId))).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? "
                        + "AND action = 'INVENTORY_MOVEMENT_REJECTED' AND outcome = 'REJECTED'",
                Integer.class, "inventory-stock-rejected")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE correlation_id = ? AND outcome = 'SUCCESS'",
                Integer.class, "inventory-stock-rejected")).isZero();
    }

    @Test
    void invalidPayloadsAndForbiddenProductionTypeLeaveInventoryUnchanged() {
        String materialId = createMaterial("MAT-INV-03", "Screw", "PIECE", "inventory-material-003");
        List<Map<String, Object>> invalidBodies = List.of(
                body(materialId, "ENTRY", "0", "REF-0", null),
                body(materialId, "ENTRY", "-1", "REF-N", null),
                body(materialId, "ENTRY", "1.0000001", "REF-S", null),
                body(materialId, "ENTRY", "10000000000000", "REF-M", null),
                body(materialId, "ENTRY", "1", null, null));
        int sequence = 0;
        for (Map<String, Object> body : invalidBodies) {
            String correlationId = "inventory-invalid-" + ++sequence;
            ResponseEntity<JsonNode> response = exchange(
                    "/api/v1/inventory/movements", HttpMethod.POST, body, operatorToken, correlationId);
            assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", correlationId);
        }

        ResponseEntity<JsonNode> production = movement(
                materialId, "PRODUCTION_CONSUMPTION", "1", "ORDER-001", null,
                operatorToken, "inventory-invalid-production");
        assertError(production, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "inventory-invalid-production");

        Map<String, Object> unknownField = new LinkedHashMap<>(body(materialId, "ENTRY", "1", "REF-U", null));
        unknownField.put("direction", "IN");
        ResponseEntity<JsonNode> unknown = exchange(
                "/api/v1/inventory/movements", HttpMethod.POST, unknownField,
                operatorToken, "inventory-invalid-unknown");
        assertError(unknown, HttpStatus.BAD_REQUEST, "UNKNOWN_FIELD", "inventory-invalid-unknown");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_movements", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT on_hand_quantity FROM inventory_balances WHERE material_id = ?",
                String.class, Long.parseLong(materialId))).isEqualTo("0.000000");
    }

    @Test
    void missingInactiveAndUnitSensitiveMaterialsUseStableErrorsAndCompensatingAdjustments() {
        ResponseEntity<JsonNode> missing = movement(
                "999999", "ENTRY", "1", "REF-MISSING", null,
                operatorToken, "inventory-reference-missing");
        assertError(missing, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "inventory-reference-missing");

        String inactiveId = createMaterial(
                "MAT-INV-04", "Inactive", "PIECE", "inventory-material-004");
        ResponseEntity<JsonNode> deactivated = exchange(
                "/api/v1/materials/" + inactiveId,
                HttpMethod.PATCH,
                Map.of("version", "0", "active", false),
                adminToken,
                "inventory-material-deactivate");
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> inactive = movement(
                inactiveId, "ENTRY", "1", "REF-INACTIVE", null,
                operatorToken, "inventory-reference-inactive");
        assertError(inactive, HttpStatus.CONFLICT, "RESOURCE_INACTIVE", "inventory-reference-inactive");

        String adjustmentId = createMaterial(
                "MAT-INV-05", "Adjustment", "KILOGRAM", "inventory-material-005");
        ResponseEntity<JsonNode> adjustment = movement(
                adjustmentId, "ADJUSTMENT_IN", "2.5", null, "Conteo físico",
                operatorToken, "inventory-adjustment-001");
        assertThat(adjustment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(adjustment.getBody().path("type").asText()).isEqualTo("ADJUSTMENT_IN");
        assertThat(adjustment.getBody().path("reason").asText()).isEqualTo("Conteo físico");

        ResponseEntity<JsonNode> unitChange = exchange(
                "/api/v1/materials/" + adjustmentId,
                HttpMethod.PATCH,
                Map.of("version", "0", "unitOfMeasure", "PIECE"),
                adminToken,
                "inventory-unit-immutable");
        assertError(unitChange, HttpStatus.CONFLICT, "MATERIAL_UNIT_IMMUTABLE", "inventory-unit-immutable");
    }

    @Test
    void balanceAndMovementQueriesSupportFiltersPaginationDetailAndStableSort() {
        String materialId = createMaterial("MAT-INV-06", "Filtered Steel", "KILOGRAM", "inventory-material-006");
        String emptyMaterialId = createMaterial("MAT-EMPTY-06", "Empty", "PIECE", "inventory-material-006b");
        String firstMovementId = null;
        for (int index = 1; index <= 21; index++) {
            ResponseEntity<JsonNode> response = movement(
                    materialId,
                    index == 1 ? "ADJUSTMENT_IN" : "ENTRY",
                    "1",
                    "FILTER-" + index,
                    null,
                    operatorToken,
                    "inventory-filter-" + index);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            if (index == 1) {
                firstMovementId = response.getBody().path("id").asText();
            }
        }

        ResponseEntity<JsonNode> page = exchange(
                "/api/v1/inventory/movements?materialId=" + materialId
                        + "&type=ENTRY&performedBy=" + operator.id()
                        + "&from=2026-07-20T00:00:00Z&to=2030-01-01T00:00:00Z"
                        + "&page=1&size=5&sort=occurredAt,asc",
                HttpMethod.GET, null, adminToken, "inventory-filter-page");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().path("content")).hasSize(5);
        assertThat(page.getBody().path("page").path("totalElements").asLong()).isEqualTo(20);
        assertThat(page.getBody().path("page").path("number").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> detail = exchange(
                "/api/v1/inventory/movements/" + firstMovementId,
                HttpMethod.GET, null, operatorToken, "inventory-filter-detail");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("referenceCode").asText()).isEqualTo("FILTER-1");

        ResponseEntity<JsonNode> balances = exchange(
                "/api/v1/inventory/balances?search=MAT-&active=true&outOfStock=false"
                        + "&sort=availableQuantity,desc",
                HttpMethod.GET, null, operatorToken, "inventory-filter-balances");
        assertThat(balances.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(balances.getBody().path("content")).hasSize(1);
        assertThat(balances.getBody().path("content").path(0).path("material").path("id").asText())
                .isEqualTo(materialId);
        ResponseEntity<JsonNode> outOfStock = exchange(
                "/api/v1/inventory/balances?outOfStock=true",
                HttpMethod.GET, null, operatorToken, "inventory-filter-empty");
        assertThat(outOfStock.getBody().path("content").toString()).contains(emptyMaterialId);

        ResponseEntity<JsonNode> invalidRange = exchange(
                "/api/v1/inventory/movements?from=2026-07-21T00:00:00Z&to=2026-07-20T00:00:00Z",
                HttpMethod.GET, null, operatorToken, "inventory-filter-range");
        assertError(invalidRange, HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "inventory-filter-range");
    }

    @Test
    void permissionsEnforceOperatorWritesAdminAndOperatorReadsAndInspectorDenial() {
        String materialId = createMaterial("MAT-INV-07", "Permissions", "PIECE", "inventory-material-007");

        assertThat(exchange(
                "/api/v1/inventory/balances", HttpMethod.GET, null,
                adminToken, "inventory-auth-admin-read").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange(
                "/api/v1/inventory/balances", HttpMethod.GET, null,
                operatorToken, "inventory-auth-operator-read").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertError(movement(
                materialId, "ENTRY", "1", "ADMIN-DENIED", null,
                adminToken, "inventory-auth-admin-write"),
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "inventory-auth-admin-write");
        assertError(exchange(
                "/api/v1/inventory/balances", HttpMethod.GET, null,
                inspectorToken, "inventory-auth-inspector-read"),
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "inventory-auth-inspector-read");
        assertError(movement(
                materialId, "ENTRY", "1", "INSPECTOR-DENIED", null,
                inspectorToken, "inventory-auth-inspector-write"),
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "inventory-auth-inspector-write");
        assertError(exchange(
                "/api/v1/inventory/balances", HttpMethod.GET, null,
                null, "inventory-auth-anonymous"),
                HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "inventory-auth-anonymous");
    }

    @Test
    void databaseTriggersAndReconciliationQueriesProtectMovementIntegrity() {
        String materialId = createMaterial("MAT-INV-08", "Integrity", "KILOGRAM", "inventory-material-008");
        ResponseEntity<JsonNode> created = movement(
                materialId, "ENTRY", "7.25", "INTEGRITY-001", null,
                operatorToken, "inventory-integrity-001");
        long movementId = created.getBody().path("id").asLong();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE inventory_movements SET quantity = 8 WHERE id = ?", movementId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM inventory_movements WHERE id = ?", movementId))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE() "
                        + "AND trigger_name IN ('trg_inventory_movements_no_update', "
                        + "'trg_inventory_movements_no_delete')",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_balances WHERE on_hand_quantity < 0 "
                        + "OR reserved_quantity < 0 OR available_quantity < 0 "
                        + "OR available_quantity <> on_hand_quantity - reserved_quantity",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT on_hand_quantity = (SELECT SUM(CASE direction WHEN 'IN' THEN quantity ELSE -quantity END) "
                        + "FROM inventory_movements WHERE material_id = ?) "
                        + "FROM inventory_balances WHERE material_id = ?",
                Boolean.class, Long.parseLong(materialId), Long.parseLong(materialId))).isTrue();
    }

    @Test
    void concurrentOutputsSerializeAndAllowAtMostOneSuccessfulConsumption() throws Exception {
        String materialId = createMaterial("MAT-INV-09", "Concurrent", "PIECE", "inventory-material-009");
        movement(materialId, "ENTRY", "10", "RACE-SETUP", null,
                operatorToken, "inventory-race-setup");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<JsonNode>> first = executor.submit(() -> movement(
                    materialId, "MANUAL_OUT", "6", "RACE-1", null,
                    operatorToken, "inventory-race-001"));
            Future<ResponseEntity<JsonNode>> second = executor.submit(() -> movement(
                    materialId, "MANUAL_OUT", "6", "RACE-2", null,
                    operatorToken, "inventory-race-002"));
            ResponseEntity<JsonNode> firstResponse = first.get(30, TimeUnit.SECONDS);
            ResponseEntity<JsonNode> secondResponse = second.get(30, TimeUnit.SECONDS);

            assertThat(Set.of(firstResponse.getStatusCode(), secondResponse.getStatusCode()))
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
            ResponseEntity<JsonNode> conflict = firstResponse.getStatusCode() == HttpStatus.CONFLICT
                    ? firstResponse : secondResponse;
            assertThat(conflict.getBody().path("code").asText()).isEqualTo("INSUFFICIENT_STOCK");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT on_hand_quantity FROM inventory_balances WHERE material_id = ?",
                    String.class, Long.parseLong(materialId))).isEqualTo("4.000000");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory_movements WHERE material_id = ?",
                    Integer.class, Long.parseLong(materialId))).isEqualTo(2);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void runtimeOpenApiPublishesAllFiveInventoryOperations() {
        ResponseEntity<JsonNode> openApi = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode paths = openApi.getBody().path("paths");
        assertThat(paths.path("/api/v1/inventory/balances").has("get")).isTrue();
        assertThat(paths.path("/api/v1/inventory/balances/{materialId}").has("get")).isTrue();
        assertThat(paths.path("/api/v1/inventory/movements").has("get")).isTrue();
        assertThat(paths.path("/api/v1/inventory/movements").has("post")).isTrue();
        assertThat(paths.path("/api/v1/inventory/movements/{movementId}").has("get")).isTrue();
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

    private ResponseEntity<JsonNode> movement(
            String materialId,
            String type,
            String quantity,
            String referenceCode,
            String reason,
            String bearerToken,
            String correlationId) {
        return exchange(
                "/api/v1/inventory/movements",
                HttpMethod.POST,
                body(materialId, type, quantity, referenceCode, reason),
                bearerToken,
                correlationId);
    }

    private Map<String, Object> body(
            String materialId,
            String type,
            String quantity,
            String referenceCode,
            String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("materialId", materialId);
        body.put("type", type);
        body.put("quantity", quantity);
        if (referenceCode != null) {
            body.put("referenceCode", referenceCode);
        }
        if (reason != null) {
            body.put("reason", reason);
        }
        return body;
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
