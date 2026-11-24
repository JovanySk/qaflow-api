package com.qaflow.auth.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El username es obligatorio.")
        @Size(min = 3, max = 100, message = "El username debe tener entre 3 y 100 caracteres.")
        String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres.")
        String password) {

    public LoginRequest {
        username = username == null ? null : username.trim();
    }
}
