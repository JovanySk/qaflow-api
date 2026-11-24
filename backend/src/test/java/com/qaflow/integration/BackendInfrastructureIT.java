package com.qaflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.shared.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
class BackendInfrastructureIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0.42"))
            .withDatabaseName("qaflow_test")
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditEventRecorder auditEventRecorder;

    @Test
    void startsWithMySqlFlywayHealthAndOpenApi31() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdFilter.HEADER_NAME, "phase7-health-001");
        ResponseEntity<Map<String, Object>> healthResponse = restTemplate.exchange(
                new RequestEntity<>(headers, HttpMethod.GET, java.net.URI.create("/actuator/health")),
                new ParameterizedTypeReference<>() {
                });

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("phase7-health-001");
        assertThat(healthResponse.getBody()).containsEntry("status", "UP");

        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class);
        Integer domainTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'",
                Integer.class);
        assertThat(successfulMigrations).isEqualTo(1);
        assertThat(domainTables).isEqualTo(12);

        ResponseEntity<String> openApiResponse = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(openApiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApiResponse.getBody())
                .contains("\"openapi\":\"3.1.0\"")
                .contains("\"title\":\"QAFlow Manufacturing API\"")
                .contains("\"bearerAuth\"")
                .contains("\"/api/v1/auth/login\"")
                .contains("\"/api/v1/auth/me\"")
                .contains("\"/api/v1/users\"")
                .contains("\"/api/v1/users/{userId}/password\"");
    }

    @Test
    void persistsJsonAuditEventAndDatabaseRejectsMutation() {
        long auditEventId = auditEventRecorder.recordInIsolatedTransaction(new AuditEventCommand(
                null,
                null,
                null,
                "PHASE7_SMOKE",
                "SYSTEM",
                "backend",
                AuditOutcome.SUCCESS,
                Map.of("phase", 7, "evidence", "integration"),
                "/actuator/health",
                "phase7-audit-001"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT action, outcome, correlation_id, JSON_UNQUOTE(JSON_EXTRACT(summary, '$.evidence')) AS evidence "
                        + "FROM audit_events WHERE id = ?",
                auditEventId);
        assertThat(row)
                .containsEntry("action", "PHASE7_SMOKE")
                .containsEntry("outcome", "SUCCESS")
                .containsEntry("correlation_id", "phase7-audit-001")
                .containsEntry("evidence", "integration");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit_events SET action = 'MUTATED' WHERE id = ?",
                auditEventId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Audit events are immutable");
    }
}
