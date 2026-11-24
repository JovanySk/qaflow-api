package com.qaflow.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank(message = "El SKU es obligatorio.")
        @Size(max = 64, message = "El SKU no puede exceder 64 caracteres.")
        String sku,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String name,
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
        String description,
        Boolean active) {

    public CreateProductRequest {
        sku = sku == null ? null : sku.trim();
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
    }

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
