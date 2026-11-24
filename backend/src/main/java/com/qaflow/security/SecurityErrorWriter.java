package com.qaflow.security;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaflow.shared.error.ApiErrorResponse;
import com.qaflow.shared.error.ApiErrorResponseFactory;
import com.qaflow.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final ApiErrorResponseFactory errorResponseFactory;

    public SecurityErrorWriter(ObjectMapper objectMapper, ApiErrorResponseFactory errorResponseFactory) {
        this.objectMapper = objectMapper;
        this.errorResponseFactory = errorResponseFactory;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {
        String correlationId = correlationId(request);
        ApiErrorResponse body = errorResponseFactory.create(
                status,
                code,
                message,
                request.getRequestURI(),
                correlationId,
                List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public static String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value == null ? "missing-correlation-id" : value.toString();
    }
}
