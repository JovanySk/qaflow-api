package com.qaflow.product.api;

import java.util.List;

import com.qaflow.shared.api.PageMetadataResponse;

public record ProductPageResponse(List<ProductResponse> content, PageMetadataResponse page) {

    public ProductPageResponse {
        content = List.copyOf(content);
    }
}
