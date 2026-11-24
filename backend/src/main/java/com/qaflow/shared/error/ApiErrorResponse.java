package com.qaflow.shared.error;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "timestamp", "status", "error", "code", "message",
        "path", "correlationId", "fieldErrors"
})
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        List<ApiFieldError> fieldErrors) {

    public ApiErrorResponse {
        timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
        error = Objects.requireNonNull(error, "error is required");
        code = Objects.requireNonNull(code, "code is required");
        message = Objects.requireNonNull(message, "message is required");
        path = Objects.requireNonNull(path, "path is required");
        correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}

