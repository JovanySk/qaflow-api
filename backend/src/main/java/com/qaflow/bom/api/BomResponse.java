package com.qaflow.bom.api;

import java.time.Instant;
import java.util.List;

import com.qaflow.auth.api.ActorSummaryResponse;

public record BomResponse(
        String id,
        BomProductSummaryResponse product,
        int revisionNumber,
        boolean current,
        List<BomItemResponse> items,
        Instant createdAt,
        ActorSummaryResponse createdBy,
        Instant supersededAt,
        ActorSummaryResponse supersededBy) {

    public BomResponse {
        items = List.copyOf(items);
    }
}
