package com.qaflow.user.api;

import java.time.Instant;

import com.qaflow.user.domain.Role;
import com.qaflow.user.domain.UserAccount;

public record UserResponse(
        String id,
        String username,
        String displayName,
        Role role,
        boolean active,
        String version,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                Long.toString(user.id()),
                user.username(),
                user.displayName(),
                user.role(),
                user.active(),
                Long.toString(user.version()),
                user.createdAt(),
                user.updatedAt());
    }
}
