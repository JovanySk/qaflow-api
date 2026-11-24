package com.qaflow.user.api;

import com.qaflow.user.domain.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "La versión es obligatoria.")
        @Pattern(regexp = "^[0-9]+$", message = "La versión debe ser un entero no negativo.")
        String version,
        @Size(min = 1, max = 150, message = "El nombre visible debe tener entre 1 y 150 caracteres.")
        String displayName,
        Role role,
        Boolean active) {

    public UpdateUserRequest {
        version = version == null ? null : version.trim();
        displayName = displayName == null ? null : displayName.trim();
    }

    @AssertTrue(message = "Debe enviarse al menos uno de displayName, role o active.")
    public boolean isChangeRequested() {
        return displayName != null || role != null || active != null;
    }
}
