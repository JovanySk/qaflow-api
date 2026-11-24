package com.qaflow.shared.error;

import org.springframework.http.HttpStatus;

public final class BusinessConflictException extends ApplicationException {

    public BusinessConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}

