package com.qaflow.shared.error;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Las credenciales no son válidas.");
    }
}
