package com.qaflow.shared.api;

public record PageMetadataResponse(
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
