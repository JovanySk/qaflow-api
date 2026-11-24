package com.qaflow.inventory.api;

import java.util.List;

import com.qaflow.shared.api.PageMetadataResponse;

public record InventoryMovementPageResponse(
        List<InventoryMovementResponse> content,
        PageMetadataResponse page) {
}
