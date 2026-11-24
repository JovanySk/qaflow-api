package com.qaflow.audit.application;

import java.util.Map;
import java.util.Objects;

import com.qaflow.audit.domain.AuditOutcome;

public record AuditEventCommand(
        Long actorUserId,
        String actorUsernameSnapshot,
        String actorRoleSnapshot,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        Map<String, Object> summary,
        String requestPath,
        String correlationId) {

    public AuditEventCommand {
        action = requiredText(action, "action", 64);
        resourceType = requiredText(resourceType, "resourceType", 64);
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        correlationId = requiredText(correlationId, "correlationId", 64);
        actorUsernameSnapshot = optionalText(actorUsernameSnapshot, "actorUsernameSnapshot", 100);
        actorRoleSnapshot = optionalText(actorRoleSnapshot, "actorRoleSnapshot", 32);
        resourceId = optionalText(resourceId, "resourceId", 64);
        requestPath = optionalText(requestPath, "requestPath", 255);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }

    private static String requiredText(String value, String field, int maxLength) {
        String normalized = Objects.requireNonNull(value, field + " is required").trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain between 1 and " + maxLength + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be null or contain between 1 and " + maxLength + " characters");
        }
        return normalized;
    }
}

