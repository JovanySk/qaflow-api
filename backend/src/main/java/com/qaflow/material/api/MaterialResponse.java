package com.qaflow.material.api;

import java.time.Instant;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.material.domain.UnitOfMeasure;

public record MaterialResponse(
        String id,
        String code,
        String name,
        String description,
        UnitOfMeasure unitOfMeasure,
        boolean active,
        String version,
        Instant createdAt,
        ActorSummaryResponse createdBy,
        Instant updatedAt,
        ActorSummaryResponse updatedBy) {
}
