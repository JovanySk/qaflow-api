package com.qaflow.user.api;

import java.util.List;

import com.qaflow.shared.api.PageMetadataResponse;

public record UserPageResponse(List<UserResponse> content, PageMetadataResponse page) {

    public UserPageResponse {
        content = List.copyOf(content);
    }
}
