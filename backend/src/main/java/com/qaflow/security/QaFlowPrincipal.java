package com.qaflow.security;

import java.util.Objects;

import com.qaflow.user.domain.Role;

public record QaFlowPrincipal(
        long id,
        String username,
        String displayName,
        Role role,
        long authVersion) {

    public QaFlowPrincipal {
        username = Objects.requireNonNull(username, "username is required");
        displayName = Objects.requireNonNull(displayName, "displayName is required");
        role = Objects.requireNonNull(role, "role is required");
    }
}
