package com.qaflow.security;

import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditRecorder.class);

    private final AuditEventRecorder auditEventRecorder;

    public SecurityAuditRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    public void authenticationRejected(HttpServletRequest request, String reason) {
        safeRecord(new AuditEventCommand(
                null,
                null,
                null,
                "AUTHENTICATION_REJECTED",
                "SECURITY",
                null,
                AuditOutcome.REJECTED,
                Map.of("reason", reason),
                request.getRequestURI(),
                SecurityErrorWriter.correlationId(request)));
    }

    public void accessDenied(HttpServletRequest request, Authentication authentication) {
        QaFlowPrincipal principal = authentication != null && authentication.getPrincipal() instanceof QaFlowPrincipal value
                ? value
                : null;
        safeRecord(new AuditEventCommand(
                principal == null ? null : principal.id(),
                principal == null ? null : principal.username(),
                principal == null ? null : principal.role().name(),
                "ACCESS_DENIED",
                "SECURITY",
                request.getRequestURI(),
                AuditOutcome.REJECTED,
                Map.of("method", request.getMethod()),
                request.getRequestURI(),
                SecurityErrorWriter.correlationId(request)));
    }

    private void safeRecord(AuditEventCommand command) {
        try {
            auditEventRecorder.recordInIsolatedTransaction(command);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Security audit persistence failed action={} correlationId={} exceptionType={}",
                    command.action(),
                    command.correlationId(),
                    exception.getClass().getName());
        }
    }
}
