package com.qaflow.inventory.api;

import java.time.Instant;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.material.api.MaterialSummaryResponse;

public record InventoryBalanceResponse(
        MaterialSummaryResponse material,
        String onHandQuantity,
        String reservedQuantity,
        String availableQuantity,
        String version,
        Instant updatedAt,
        ActorSummaryResponse updatedBy) {
}
