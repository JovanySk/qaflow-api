package com.qaflow.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "La versión es obligatoria.")
        @Pattern(regexp = "^[0-9]+$", message = "La versión debe ser un entero no negativo.")
        String version,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 128, message = "La nueva contraseña debe tener entre 8 y 128 caracteres.")
        String newPassword) {

    public ChangePasswordRequest {
        version = version == null ? null : version.trim();
    }
}
