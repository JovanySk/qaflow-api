package com.qaflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ActiveUserJwtValidatorTest {

    private UserAccountRepository repository;
    private ActiveUserJwtValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        validator = new ActiveUserJwtValidator(repository);
    }

    @Test
    void acceptsCurrentActiveUserRoleAndAuthorizationVersion() {
        UserAccount user = mockUser(true, Role.OPERATOR, 4);
        when(repository.findById(9L)).thenReturn(Optional.of(user));

        assertThat(validator.validate(jwt("9", Role.OPERATOR, 4)).hasErrors()).isFalse();
    }

    @Test
    void rejectsInactiveUserChangedRoleAndStaleAuthorizationVersion() {
        UserAccount inactive = mockUser(false, Role.OPERATOR, 4);
        when(repository.findById(9L)).thenReturn(Optional.of(inactive));
        assertThat(validator.validate(jwt("9", Role.OPERATOR, 4)).hasErrors()).isTrue();

        UserAccount active = mockUser(true, Role.ADMIN, 5);
        when(repository.findById(9L)).thenReturn(Optional.of(active));
        assertThat(validator.validate(jwt("9", Role.OPERATOR, 5)).hasErrors()).isTrue();
        assertThat(validator.validate(jwt("9", Role.ADMIN, 4)).hasErrors()).isTrue();
    }

    private UserAccount mockUser(boolean active, Role role, long authVersion) {
        UserAccount user = mock(UserAccount.class);
        when(user.active()).thenReturn(active);
        when(user.role()).thenReturn(role);
        when(user.authVersion()).thenReturn(authVersion);
        return user;
    }

    private Jwt jwt(String subject, Role role, long authVersion) {
        Instant now = Instant.now();
        return new Jwt(
                "token",
                now,
                now.plusSeconds(900),
                Map.of("alg", "HS256"),
                Map.of("sub", subject, "role", role.name(), "authVersion", authVersion));
    }
}
