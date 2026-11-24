package com.qaflow.bom.api;

import com.qaflow.shared.validation.ExactQuantityParser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BomItemRequest(
        @NotBlank(message = "El material es obligatorio.")
        @Pattern(regexp = "^[1-9][0-9]*$", message = "materialId debe ser un entero positivo.")
        String materialId,
        @NotBlank(message = "La cantidad por unidad es obligatoria.")
        @Pattern(
                regexp = ExactQuantityParser.POSITIVE_PATTERN,
                message = "quantityPerUnit debe ser positiva y tener hasta 13 enteros y 6 decimales.")
        String quantityPerUnit) {

    public BomItemRequest {
        materialId = materialId == null ? null : materialId.trim();
        quantityPerUnit = quantityPerUnit == null ? null : quantityPerUnit.trim();
    }
}
