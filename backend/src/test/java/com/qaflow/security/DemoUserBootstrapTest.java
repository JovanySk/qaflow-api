package com.qaflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoUserBootstrapTest {

    private Environment environment;
    private UserAccountRepository repository;
    private PasswordEncoder passwordEncoder;
    private AuditEventRecorder auditEventRecorder;
    private BootstrapProperties properties;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        repository = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        properties = new BootstrapProperties(
                true,
                "admin.demo", "Demo Administrator", "Admin123!",
                "operator.demo", "Demo Operator", "Operator123!",
                "inspector.demo", "Demo Inspector", "Inspector123!");
    }

    @Test
    void createsOneActiveUserForEveryApprovedRoleWithoutPersistingPlaintext() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"local"});
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}encoded-value");
        DemoUserBootstrap bootstrap = bootstrap();

        bootstrap.run(mock(ApplicationArguments.class));

        ArgumentCaptor<UserAccount> users = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository, times(3)).save(users.capture());
        verify(repository).flush();
        assertThat(users.getAllValues()).extracting(UserAccount::role)
                .containsExactly(Role.ADMIN, Role.OPERATOR, Role.QUALITY_INSPECTOR);
        assertThat(users.getAllValues()).allSatisfy(user -> {
            assertThat(user.active()).isTrue();
            assertThat(user.passwordHash()).isEqualTo("{bcrypt}encoded-value");
            assertThat(user.passwordHash()).doesNotContain("123!");
        });
        verify(auditEventRecorder).recordWithinBusinessTransaction(any());
    }

    @Test
    void refusesDemoBootstrapInProductionProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

        assertThatThrownBy(() -> bootstrap().run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Demo user bootstrap is forbidden in the prod profile");

        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    private DemoUserBootstrap bootstrap() {
        return new DemoUserBootstrap(
                properties,
                environment,
                repository,
                passwordEncoder,
                auditEventRecorder,
                Clock.fixed(Instant.parse("2026-07-20T03:00:00Z"), ZoneOffset.UTC));
    }
}
