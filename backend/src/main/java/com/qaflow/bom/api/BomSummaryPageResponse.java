package com.qaflow.bom.api;

import java.util.List;

import com.qaflow.shared.api.PageMetadataResponse;

public record BomSummaryPageResponse(List<BomSummaryResponse> content, PageMetadataResponse page) {

    public BomSummaryPageResponse {
        content = List.copyOf(content);
    }
}
