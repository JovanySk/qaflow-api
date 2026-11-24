package com.qaflow.inventory.api;

import com.qaflow.shared.validation.ExactQuantityParser;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateInventoryMovementRequest(
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]*$", message = "materialId debe ser un entero positivo.")
        String materialId,

        @NotBlank
        @Pattern(
                regexp = "^(ENTRY|MANUAL_OUT|ADJUSTMENT_IN|ADJUSTMENT_OUT|PRODUCTION_CONSUMPTION)$",
                message = "type no pertenece al catálogo permitido.")
        String type,

        @NotBlank
        @Pattern(
                regexp = ExactQuantityParser.POSITIVE_PATTERN,
                message = "quantity debe ser positiva con hasta 13 enteros y 6 decimales.")
        String quantity,

        @Size(max = 120)
        @Pattern(regexp = "(?s)^.*\\S.*$", message = "referenceCode no puede estar vacío.")
        String referenceCode,

        @Size(max = 500)
        @Pattern(regexp = "(?s)^.*\\S.*$", message = "reason no puede estar vacío.")
        String reason) {

    @AssertTrue(message = "Debe informarse referenceCode o reason.")
    public boolean isReferenceOrReasonPresent() {
        return hasText(referenceCode) || hasText(reason);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
