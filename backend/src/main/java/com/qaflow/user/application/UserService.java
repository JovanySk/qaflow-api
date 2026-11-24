package com.qaflow.user.application;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.api.PageMetadataResponse;
import com.qaflow.shared.error.BadRequestException;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.error.ResourceNotFoundException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.api.ChangePasswordRequest;
import com.qaflow.user.api.CreateUserRequest;
import com.qaflow.user.api.UpdateUserRequest;
import com.qaflow.user.api.UserPageResponse;
import com.qaflow.user.api.UserResponse;
import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;
import com.qaflow.user.infrastructure.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventRecorder auditEventRecorder;
    private final DeferredAuditEventRecorder deferredAuditEventRecorder;
    private final Clock clock;

    public UserService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuditEventRecorder auditEventRecorder,
            DeferredAuditEventRecorder deferredAuditEventRecorder,
            Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventRecorder = auditEventRecorder;
        this.deferredAuditEventRecorder = deferredAuditEventRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(
            int page,
            int size,
            String sort,
            String search,
            Role role,
            Boolean active) {
        Specification<UserAccount> specification = (root, query, builder) -> builder.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.get("usernameNormalized"), pattern),
                    builder.like(builder.upper(root.get("displayName")), pattern)));
        }
        if (role != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("role"), role));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }

        Page<UserAccount> result = userAccountRepository.findAll(
                specification,
                PageRequest.of(page, size, UserSort.parse(sort)));
        return new UserPageResponse(
                result.getContent().stream().map(UserResponse::from).toList(),
                new PageMetadataResponse(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()));
    }

    @Transactional(readOnly = true)
    public UserResponse get(String userId) {
        UserAccount user = userAccountRepository.findById(parseId(userId))
                .orElseThrow(() -> userNotFound(userId));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(
            CreateUserRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        String normalizedUsername = request.username().toUpperCase(Locale.ROOT);
        if (userAccountRepository.existsByUsernameNormalized(normalizedUsername)) {
            reject(actor, "USER_CREATE_REJECTED", null, "DUPLICATE_USERNAME", metadata);
            throw duplicateUsername();
        }

        Instant now = Instant.now(clock);
        UserAccount user = UserAccount.create(
                request.username(),
                request.displayName(),
                passwordEncoder.encode(request.password()),
                request.role(),
                request.activeOrDefault(),
                now,
                actor.id());
        try {
            userAccountRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            reject(actor, "USER_CREATE_REJECTED", null, "DUPLICATE_USERNAME", metadata);
            throw duplicateUsername();
        }
        recordSuccess(
                actor,
                "USER_CREATED",
                user,
                Map.of("username", user.username(), "role", user.role().name(), "active", user.active()),
                metadata);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(
            String userId,
            UpdateUserRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long id = parseId(userId);
        userAccountRepository.lockActiveAdministrators();
        UserAccount user = userAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> userNotFound(userId));
        verifyVersion(user, request.version(), actor, "USER_UPDATE_REJECTED", metadata);

        boolean removesActiveAdministrator = user.active()
                && user.role() == Role.ADMIN
                && (Boolean.FALSE.equals(request.active())
                        || request.role() != null && request.role() != Role.ADMIN);
        if (removesActiveAdministrator && userAccountRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            reject(actor, "USER_UPDATE_REJECTED", userId, "LAST_ACTIVE_ADMIN", metadata);
            throw new BusinessConflictException(
                    "LAST_ACTIVE_ADMIN",
                    "No se puede desactivar o degradar al último ADMIN activo.");
        }

        boolean securityChanged = user.update(
                request.displayName(),
                request.role(),
                request.active(),
                Instant.now(clock),
                actor.id());
        userAccountRepository.saveAndFlush(user);
        Map<String, Object> summary = new HashMap<>();
        summary.put("role", user.role().name());
        summary.put("active", user.active());
        summary.put("securityChanged", securityChanged);
        recordSuccess(actor, "USER_UPDATED", user, summary, metadata);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(
            String userId,
            ChangePasswordRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long id = parseId(userId);
        UserAccount user = userAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> userNotFound(userId));
        verifyVersion(user, request.version(), actor, "USER_PASSWORD_CHANGE_REJECTED", metadata);
        user.changePassword(passwordEncoder.encode(request.newPassword()), Instant.now(clock), actor.id());
        userAccountRepository.saveAndFlush(user);
        recordSuccess(
                actor,
                "USER_PASSWORD_CHANGED",
                user,
                Map.of("authVersionIncremented", true),
                metadata);
    }

    private void verifyVersion(
            UserAccount user,
            String requestedVersion,
            QaFlowPrincipal actor,
            String rejectedAction,
            RequestMetadata metadata) {
        long version = parseVersion(requestedVersion);
        if (user.version() != version) {
            reject(actor, rejectedAction, Long.toString(user.id()), "CONCURRENT_MODIFICATION", metadata);
            throw new BusinessConflictException(
                    "CONCURRENT_MODIFICATION",
                    "El recurso fue modificado por otra operación. Actualiza los datos e intenta de nuevo.");
        }
    }

    private void recordSuccess(
            QaFlowPrincipal actor,
            String action,
            UserAccount target,
            Map<String, Object> summary,
            RequestMetadata metadata) {
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                action,
                "USER",
                Long.toString(target.id()),
                AuditOutcome.SUCCESS,
                summary,
                metadata.path(),
                metadata.correlationId()));
    }

    private void reject(
            QaFlowPrincipal actor,
            String action,
            String resourceId,
            String reason,
            RequestMetadata metadata) {
        deferredAuditEventRecorder.recordAfterCurrentTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                action,
                "USER",
                resourceId,
                AuditOutcome.REJECTED,
                Map.of("reason", reason),
                metadata.path(),
                metadata.correlationId()));
    }

    private long parseId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("INVALID_PATH_PARAMETER", "userId debe ser un entero positivo.");
        }
    }

    private long parseVersion(String version) {
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("VALIDATION_ERROR", "La versión debe ser un entero no negativo.");
        }
    }

    private ResourceNotFoundException userNotFound(String userId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                "No existe un usuario con id " + userId + ".");
    }

    private BusinessConflictException duplicateUsername() {
        return new BusinessConflictException(
                "DUPLICATE_USERNAME",
                "Ya existe un usuario con el username indicado.");
    }
}
