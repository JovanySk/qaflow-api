package com.qaflow.bom.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublishBomRequest(
        @NotNull(message = "La lista de materiales es obligatoria.")
        @Size(min = 1, max = 100, message = "La BOM debe contener entre 1 y 100 materiales.")
        List<@Valid BomItemRequest> items) {

    public PublishBomRequest {
        items = items == null ? null : List.copyOf(items);
    }
}
