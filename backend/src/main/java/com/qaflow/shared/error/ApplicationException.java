package com.qaflow.shared.error;

import java.util.Objects;

import org.springframework.http.HttpStatus;

public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApplicationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "status is required");
        this.code = Objects.requireNonNull(code, "code is required");
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}

