package com.qaflow.inventory.api;

import java.time.Instant;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.inventory.domain.InventoryDirection;
import com.qaflow.inventory.domain.InventoryMovementType;
import com.qaflow.material.api.MaterialSummaryResponse;

public record InventoryMovementResponse(
        String id,
        MaterialSummaryResponse material,
        InventoryMovementType type,
        InventoryDirection direction,
        String quantity,
        String onHandAfter,
        String reservedAfter,
        String productionOrderId,
        String referenceCode,
        String reason,
        ActorSummaryResponse performedBy,
        Instant occurredAt,
        String correlationId) {
}
