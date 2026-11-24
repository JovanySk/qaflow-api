package com.qaflow.shared.web;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

public record RequestMetadata(String path, String correlationId) {

    public RequestMetadata {
        path = Objects.requireNonNull(path, "path is required");
        correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
    }

    public static RequestMetadata from(HttpServletRequest request) {
        Object correlationValue = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        String correlationId = correlationValue == null ? "missing-correlation-id" : correlationValue.toString();
        return new RequestMetadata(request.getRequestURI(), correlationId);
    }
}
