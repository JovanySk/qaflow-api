package com.qaflow.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qaflow.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "El username es obligatorio.")
        @Size(min = 3, max = 100, message = "El username debe tener entre 3 y 100 caracteres.")
        String username,
        @NotBlank(message = "El nombre visible es obligatorio.")
        @Size(max = 150, message = "El nombre visible no puede exceder 150 caracteres.")
        String displayName,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres.")
        String password,
        @NotNull(message = "El rol es obligatorio.")
        Role role,
        Boolean active) {

    public CreateUserRequest {
        username = username == null ? null : username.trim();
        displayName = displayName == null ? null : displayName.trim();
    }

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
