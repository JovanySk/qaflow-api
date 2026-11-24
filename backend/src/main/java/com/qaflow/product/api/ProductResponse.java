package com.qaflow.product.api;

import java.time.Instant;

import com.qaflow.auth.api.ActorSummaryResponse;

public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        boolean active,
        String version,
        Instant createdAt,
        ActorSummaryResponse createdBy,
        Instant updatedAt,
        ActorSummaryResponse updatedBy) {
}
