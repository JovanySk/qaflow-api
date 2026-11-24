package com.qaflow.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
class SecurityApiIT {

    private static final String TEST_PASSWORD = "password";
    private static final String TEST_PASSWORD_HASH =
            "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.42"))
            .withDatabaseName("qaflow_security_test")
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
    private UserAccount inactive;

    @BeforeEach
    void setUpUsers() {
        truncateUsersAndAudit();
        Instant now = Instant.parse("2026-07-20T03:00:00Z");
        admin = userAccountRepository.save(UserAccount.create(
                "admin.demo", "Demo Administrator", TEST_PASSWORD_HASH, Role.ADMIN, true, now, null));
        operator = userAccountRepository.save(UserAccount.create(
                "operator.demo", "Demo Operator", TEST_PASSWORD_HASH, Role.OPERATOR, true, now, null));
        inactive = userAccountRepository.save(UserAccount.create(
                "inactive.demo", "Inactive User", TEST_PASSWORD_HASH, Role.OPERATOR, false, now, null));
        userAccountRepository.flush();
    }

    @Test
    void loginAndMeReturnContractWithBearerToken() {
        ResponseEntity<JsonNode> login = login("admin.demo", TEST_PASSWORD, "api-login-001");

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(login.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo("api-login-001");
        assertThat(login.getBody().path("tokenType").asText()).isEqualTo("BEARER");
        assertThat(login.getBody().path("expiresInSeconds").asLong()).isEqualTo(900);
        assertThat(login.getBody().path("user").path("role").asText()).isEqualTo("ADMIN");
        assertThat(login.getBody().path("accessToken").asText()).isNotBlank();

        ResponseEntity<JsonNode> me = exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                null,
                login.getBody().path("accessToken").asText(),
                "api-me-001");
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().path("user").path("id").asText()).isEqualTo(admin.id().toString());
        assertThat(me.getBody().path("user").path("username").asText()).isEqualTo("admin.demo");
    }

    @Test
    void badUnknownAndInactiveCredentialsShareGenericUnauthorizedResponse() {
        ResponseEntity<JsonNode> wrongPassword = login("admin.demo", "wrong-pass", "api-login-002");
        ResponseEntity<JsonNode> missingUser = login("missing.demo", "wrong-pass", "api-login-003");
        ResponseEntity<JsonNode> inactiveUser = login("inactive.demo", TEST_PASSWORD, "api-login-004");

        for (ResponseEntity<JsonNode> response : List.of(wrongPassword, missingUser, inactiveUser)) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
            assertThat(response.getBody().path("code").asText()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(response.getBody().path("message").asText()).isEqualTo("Las credenciales no son válidas.");
            assertThat(response.getBody().toString()).doesNotContain("password", "inactive", "missing.demo");
        }
        Integer rejectedLogins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action = 'LOGIN_REJECTED' AND outcome = 'REJECTED'",
                Integer.class);
        assertThat(rejectedLogins).isEqualTo(3);
    }

    @Test
    void missingMalformedExpiredTokensAndWrongRoleReturn401Or403() {
        ResponseEntity<JsonNode> missing = exchange(
                "/api/v1/users", HttpMethod.GET, null, null, "api-sec-001");
        assertSecurityError(missing, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "api-sec-001");

        ResponseEntity<JsonNode> malformed = exchange(
                "/api/v1/users", HttpMethod.GET, null, "not-a-jwt", "api-sec-002");
        assertSecurityError(malformed, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "api-sec-002");

        String adminToken = accessToken(login("admin.demo", TEST_PASSWORD, "api-sec-login-001"));
        ResponseEntity<JsonNode> altered = exchange(
                "/api/v1/users", HttpMethod.GET, null, adminToken + "x", "api-sec-003");
        assertSecurityError(altered, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "api-sec-003");

        ResponseEntity<JsonNode> expired = exchange(
                "/api/v1/auth/me", HttpMethod.GET, null, expiredToken(admin), "api-sec-004");
        assertSecurityError(expired, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "api-sec-004");

        String operatorToken = accessToken(login("operator.demo", TEST_PASSWORD, "api-sec-login-002"));
        ResponseEntity<JsonNode> forbidden = exchange(
                "/api/v1/users", HttpMethod.GET, null, operatorToken, "api-sec-005");
        assertSecurityError(forbidden, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "api-sec-005");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action = 'ACCESS_DENIED' AND actor_user_id = ?",
                Integer.class,
                operator.id())).isEqualTo(1);
    }

    @Test
    void adminCreatesUpdatesAndChangesPasswordWhileOldTokenIsRevoked() {
        String adminToken = accessToken(login("admin.demo", TEST_PASSWORD, "api-user-login-001"));
        Map<String, Object> createBody = Map.of(
                "username", "new.operator",
                "displayName", "New Operator",
                "password", "Initial123!",
                "role", "OPERATOR",
                "active", true);
        ResponseEntity<JsonNode> created = exchange(
                "/api/v1/users", HttpMethod.POST, createBody, adminToken, "api-user-001");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("version").asText()).isEqualTo("0");
        assertThat(created.getBody().has("password")).isFalse();
        assertThat(created.getBody().has("passwordHash")).isFalse();
        String userId = created.getBody().path("id").asText();
        assertThat(created.getHeaders().getLocation()).hasToString("/api/v1/users/" + userId);

        ResponseEntity<JsonNode> detail = exchange(
                "/api/v1/users/" + userId, HttpMethod.GET, null, adminToken, "api-user-detail-001");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("username").asText()).isEqualTo("new.operator");

        ResponseEntity<JsonNode> page = exchange(
                "/api/v1/users?search=new&role=OPERATOR&active=true&page=0&size=20&sort=username,asc",
                HttpMethod.GET,
                null,
                adminToken,
                "api-user-list-001");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().path("content").size()).isEqualTo(1);
        assertThat(page.getBody().path("content").path(0).path("id").asText()).isEqualTo(userId);
        assertThat(page.getBody().path("page").path("totalElements").asLong()).isEqualTo(1);

        Map<String, Object> duplicateBody = Map.of(
                "username", " NEW.OPERATOR ",
                "displayName", "Duplicate",
                "password", "Initial123!",
                "role", "OPERATOR");
        ResponseEntity<JsonNode> duplicate = exchange(
                "/api/v1/users", HttpMethod.POST, duplicateBody, adminToken, "api-user-002");
        assertSecurityError(duplicate, HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "api-user-002");

        String userToken = accessToken(login("new.operator", "Initial123!", "api-user-login-002"));
        ResponseEntity<JsonNode> updated = exchange(
                "/api/v1/users/" + userId,
                HttpMethod.PATCH,
                Map.of("version", "0", "role", "QUALITY_INSPECTOR"),
                adminToken,
                "api-user-003");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("version").asText()).isEqualTo("1");
        assertThat(updated.getBody().path("role").asText()).isEqualTo("QUALITY_INSPECTOR");

        ResponseEntity<JsonNode> revoked = exchange(
                "/api/v1/auth/me", HttpMethod.GET, null, userToken, "api-user-004");
        assertSecurityError(revoked, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "api-user-004");

        ResponseEntity<JsonNode> passwordChanged = exchange(
                "/api/v1/users/" + userId + "/password",
                HttpMethod.PATCH,
                Map.of("version", "1", "newPassword", "Changed123!"),
                adminToken,
                "api-user-005");
        assertThat(passwordChanged.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login("new.operator", "Initial123!", "api-user-login-003").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login("new.operator", "Changed123!", "api-user-login-004").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void lastAdminAndStaleVersionProduceBusinessConflicts() {
        String adminToken = accessToken(login("admin.demo", TEST_PASSWORD, "api-concurrency-login-001"));
        String adminId = admin.id().toString();

        ResponseEntity<JsonNode> lastAdmin = exchange(
                "/api/v1/users/" + adminId,
                HttpMethod.PATCH,
                Map.of("version", "0", "role", "OPERATOR"),
                adminToken,
                "api-concurrency-001");
        assertSecurityError(lastAdmin, HttpStatus.CONFLICT, "LAST_ACTIVE_ADMIN", "api-concurrency-001");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events "
                        + "WHERE action = 'USER_UPDATE_REJECTED' AND outcome = 'REJECTED' AND correlation_id = ?",
                Integer.class,
                "api-concurrency-001")).isEqualTo(1);

        ResponseEntity<JsonNode> firstUpdate = exchange(
                "/api/v1/users/" + adminId,
                HttpMethod.PATCH,
                Map.of("version", "0", "displayName", "Updated Administrator"),
                adminToken,
                "api-concurrency-002");
        assertThat(firstUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstUpdate.getBody().path("version").asText()).isEqualTo("1");

        ResponseEntity<JsonNode> staleUpdate = exchange(
                "/api/v1/users/" + adminId,
                HttpMethod.PATCH,
                Map.of("version", "0", "displayName", "Stale Update"),
                adminToken,
                "api-concurrency-003");
        assertSecurityError(
                staleUpdate, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "api-concurrency-003");
    }

    @Test
    void invalidAndUnknownRequestDataReturnSanitized400() {
        String adminToken = accessToken(login("admin.demo", TEST_PASSWORD, "api-validation-login-001"));
        Map<String, Object> invalid = Map.of(
                "username", "x",
                "displayName", "",
                "password", "secret",
                "role", "OPERATOR");
        ResponseEntity<JsonNode> invalidResponse = exchange(
                "/api/v1/users", HttpMethod.POST, invalid, adminToken, "api-validation-001");
        assertSecurityError(invalidResponse, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "api-validation-001");
        assertThat(invalidResponse.getBody().path("fieldErrors").size()).isGreaterThanOrEqualTo(3);
        assertThat(invalidResponse.getBody().toString()).doesNotContain("secret");

        Map<String, Object> unknownField = Map.of(
                "username", "valid.user",
                "displayName", "Valid User",
                "password", "Initial123!",
                "role", "OPERATOR",
                "passwordHash", "must-not-be-accepted");
        ResponseEntity<JsonNode> unknownResponse = exchange(
                "/api/v1/users", HttpMethod.POST, unknownField, adminToken, "api-validation-002");
        assertSecurityError(unknownResponse, HttpStatus.BAD_REQUEST, "UNKNOWN_FIELD", "api-validation-002");
        assertThat(unknownResponse.getBody().toString()).doesNotContain("must-not-be-accepted");
    }

    private ResponseEntity<JsonNode> login(String username, String password, String correlationId) {
        return exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                Map.of("username", username, "password", password),
                null,
                correlationId);
    }

    private String accessToken(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("accessToken").asText();
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

    private void assertSecurityError(
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

    private String expiredToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .subject(user.id().toString())
                .issuedAt(now.minusSeconds(1200))
                .expiresAt(now.minusSeconds(60))
                .id(UUID.randomUUID().toString())
                .claim("role", user.role().name())
                .claim("authVersion", user.authVersion())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    private void truncateUsersAndAudit() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                statement.execute("TRUNCATE TABLE audit_events");
                statement.execute("TRUNCATE TABLE users");
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }
}
