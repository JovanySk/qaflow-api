package com.qaflow.shared.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaflow.shared.error.ApiErrorResponse;
import com.qaflow.shared.error.ApiErrorResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private final ObjectMapper objectMapper;
    private final ApiErrorResponseFactory errorResponseFactory;

    public CorrelationIdFilter(ObjectMapper objectMapper, ApiErrorResponseFactory errorResponseFactory) {
        this.objectMapper = objectMapper;
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestedCorrelationId = request.getHeader(HEADER_NAME);
        if (requestedCorrelationId != null && !VALID_CORRELATION_ID.matcher(requestedCorrelationId).matches()) {
            String generatedCorrelationId = generateCorrelationId();
            bindCorrelationId(request, response, generatedCorrelationId);
            writeInvalidCorrelationId(request, response, generatedCorrelationId);
            return;
        }

        String correlationId = requestedCorrelationId == null
                ? generateCorrelationId()
                : requestedCorrelationId;
        bindCorrelationId(request, response, correlationId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            filterChain.doFilter(request, response);
        }
    }

    private void bindCorrelationId(
            HttpServletRequest request,
            HttpServletResponse response,
            String correlationId) {
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
    }

    private void writeInvalidCorrelationId(
            HttpServletRequest request,
            HttpServletResponse response,
            String correlationId) throws IOException {
        ApiErrorResponse body = errorResponseFactory.create(
                HttpStatus.BAD_REQUEST,
                "INVALID_CORRELATION_ID",
                "X-Correlation-Id debe contener entre 1 y 64 caracteres alfanuméricos, punto, guion o guion bajo.",
                request.getRequestURI(),
                correlationId,
                List.of());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}

