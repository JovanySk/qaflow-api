package com.qaflow.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class QaFlowAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter securityErrorWriter;
    private final SecurityAuditRecorder securityAuditRecorder;

    public QaFlowAccessDeniedHandler(
            SecurityErrorWriter securityErrorWriter,
            SecurityAuditRecorder securityAuditRecorder) {
        this.securityErrorWriter = securityErrorWriter;
        this.securityAuditRecorder = securityAuditRecorder;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        securityAuditRecorder.accessDenied(request, authentication);
        securityErrorWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "El usuario autenticado no tiene permiso para esta operación.");
    }
}
