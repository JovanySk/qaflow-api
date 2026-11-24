package com.qaflow.auth.application;

import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.auth.api.LoginRequest;
import com.qaflow.auth.api.LoginResponse;
import com.qaflow.security.JwtTokenService;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.security.UserAccountDetails;
import com.qaflow.shared.error.InvalidCredentialsException;
import com.qaflow.shared.web.RequestMetadata;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AuditEventRecorder auditEventRecorder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            AuditEventRecorder auditEventRecorder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, RequestMetadata metadata) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException exception) {
            auditEventRecorder.recordInIsolatedTransaction(new AuditEventCommand(
                    null,
                    null,
                    null,
                    "LOGIN_REJECTED",
                    "SECURITY",
                    null,
                    AuditOutcome.REJECTED,
                    Map.of("reason", "INVALID_CREDENTIALS"),
                    metadata.path(),
                    metadata.correlationId()));
            throw new InvalidCredentialsException();
        }

        UserAccountDetails details = (UserAccountDetails) authentication.getPrincipal();
        QaFlowPrincipal principal = details.principal();
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(principal);
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                principal.id(),
                principal.username(),
                principal.role().name(),
                "LOGIN_SUCCEEDED",
                "USER",
                Long.toString(principal.id()),
                AuditOutcome.SUCCESS,
                Map.of("role", principal.role().name()),
                metadata.path(),
                metadata.correlationId()));
        return new LoginResponse(
                issuedToken.value(),
                "BEARER",
                issuedToken.expiresInSeconds(),
                ActorSummaryResponse.from(principal));
    }
}
