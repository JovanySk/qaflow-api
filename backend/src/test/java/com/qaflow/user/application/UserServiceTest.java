package com.qaflow.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.api.ChangePasswordRequest;
import com.qaflow.user.api.CreateUserRequest;
import com.qaflow.user.api.UpdateUserRequest;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T03:00:00Z");
    private static final QaFlowPrincipal ACTOR =
            new QaFlowPrincipal(1, "admin.demo", "Administrator", Role.ADMIN, 0);
    private static final RequestMetadata METADATA =
            new RequestMetadata("/api/v1/users", "unit-user-001");

    private UserAccountRepository repository;
    private PasswordEncoder passwordEncoder;
    private AuditEventRecorder auditEventRecorder;
    private DeferredAuditEventRecorder deferredAuditEventRecorder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        deferredAuditEventRecorder = mock(DeferredAuditEventRecorder.class);
        service = new UserService(
                repository,
                passwordEncoder,
                auditEventRecorder,
                deferredAuditEventRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsNormalizedDuplicateUsernameBeforeHashingPassword() {
        when(repository.existsByUsernameNormalized("NEW.USER")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateUserRequest(" new.user ", "New User", "Demo123!", Role.OPERATOR, true),
                ACTOR,
                METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("DUPLICATE_USERNAME"));

        verify(passwordEncoder, never()).encode(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void preventsRemovingTheLastActiveAdministrator() {
        UserAccount target = mock(UserAccount.class);
        when(target.id()).thenReturn(1L);
        when(target.active()).thenReturn(true);
        when(target.role()).thenReturn(Role.ADMIN);
        when(target.version()).thenReturn(2L);
        when(repository.lockActiveAdministrators()).thenReturn(List.of(target));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(target));
        when(repository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(
                "1",
                new UpdateUserRequest("2", null, Role.OPERATOR, null),
                ACTOR,
                METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("LAST_ACTIVE_ADMIN"));

        verify(target, never()).update(any(), any(), any(), any(), any());
    }

    @Test
    void hashesPasswordAndPersistsWithoutAuditingTheSecret() {
        UserAccount target = mock(UserAccount.class);
        when(target.id()).thenReturn(2L);
        when(target.version()).thenReturn(4L);
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode("NewDemo123!")).thenReturn("{bcrypt}encoded");

        service.changePassword(
                "2",
                new ChangePasswordRequest("4", "NewDemo123!"),
                ACTOR,
                new RequestMetadata("/api/v1/users/2/password", "unit-user-002"));

        verify(target).changePassword("{bcrypt}encoded", NOW, ACTOR.id());
        verify(repository).saveAndFlush(target);
        verify(auditEventRecorder).recordWithinBusinessTransaction(any());
    }
}
