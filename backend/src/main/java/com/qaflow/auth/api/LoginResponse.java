package com.qaflow.auth.api;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        ActorSummaryResponse user) {
}
