package com.qaflow.material.api;

import com.qaflow.material.domain.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaterialRequest(
        @NotBlank(message = "El código es obligatorio.")
        @Size(max = 64, message = "El código no puede exceder 64 caracteres.")
        String code,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String name,
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
        String description,
        @NotNull(message = "La unidad de medida es obligatoria.")
        UnitOfMeasure unitOfMeasure,
        Boolean active) {

    public CreateMaterialRequest {
        code = code == null ? null : code.trim();
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
    }

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
