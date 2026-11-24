package com.qaflow.security;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class QaFlowAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter securityErrorWriter;
    private final SecurityAuditRecorder securityAuditRecorder;

    public QaFlowAuthenticationEntryPoint(
            SecurityErrorWriter securityErrorWriter,
            SecurityAuditRecorder securityAuditRecorder) {
        this.securityErrorWriter = securityErrorWriter;
        this.securityAuditRecorder = securityAuditRecorder;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        boolean expired = containsExpiredMarker(authException);
        String code = expired ? "TOKEN_EXPIRED" : "INVALID_TOKEN";
        String message = expired
                ? "El token de acceso expiró. Inicia sesión nuevamente."
                : "El token de acceso está ausente o no es válido.";
        securityAuditRecorder.authenticationRejected(request, expired ? "TOKEN_EXPIRED" : "INVALID_TOKEN");
        securityErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, code, message);
    }

    private boolean containsExpiredMarker(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 10) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("expired")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
