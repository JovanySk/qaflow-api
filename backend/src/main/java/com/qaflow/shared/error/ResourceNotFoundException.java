package com.qaflow.shared.error;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}

