package com.qaflow.shared.error;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

    private final Clock clock;

    public ApiErrorResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path,
            String correlationId,
            List<ApiFieldError> fieldErrors) {
        return new ApiErrorResponse(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                correlationId,
                fieldErrors);
    }
}

