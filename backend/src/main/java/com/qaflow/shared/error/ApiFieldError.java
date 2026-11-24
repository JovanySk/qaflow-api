package com.qaflow.shared.error;

import java.util.Objects;

public record ApiFieldError(String field, String code, String message) {

    public ApiFieldError {
        field = Objects.requireNonNull(field, "field is required");
        code = Objects.requireNonNull(code, "code is required");
        message = Objects.requireNonNull(message, "message is required");
    }
}

