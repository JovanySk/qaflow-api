package com.qaflow.bom.api;

import java.time.Instant;

import com.qaflow.auth.api.ActorSummaryResponse;

public record BomSummaryResponse(
        String id,
        int revisionNumber,
        boolean current,
        long itemCount,
        Instant createdAt,
        ActorSummaryResponse createdBy) {
}
