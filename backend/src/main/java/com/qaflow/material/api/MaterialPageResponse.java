package com.qaflow.material.api;

import java.util.List;

import com.qaflow.shared.api.PageMetadataResponse;

public record MaterialPageResponse(List<MaterialResponse> content, PageMetadataResponse page) {

    public MaterialPageResponse {
        content = List.copyOf(content);
    }
}
