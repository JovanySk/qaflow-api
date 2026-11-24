package com.qaflow.user.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "username_normalized", insertable = false, updatable = false, length = 100)
    private String usernameNormalized;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected UserAccount() {
    }

    private UserAccount(
            String username,
            String displayName,
            String passwordHash,
            Role role,
            boolean active,
            Instant occurredAt,
            Long actorUserId) {
        this.username = requiredText(username, "username");
        this.displayName = requiredText(displayName, "displayName");
        this.passwordHash = requiredText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role is required");
        this.active = active;
        this.authVersion = 0;
        this.version = 0;
        this.createdAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.createdBy = actorUserId;
        this.updatedAt = occurredAt;
        this.updatedBy = actorUserId;
    }

    public static UserAccount create(
            String username,
            String displayName,
            String passwordHash,
            Role role,
            boolean active,
            Instant occurredAt,
            Long actorUserId) {
        return new UserAccount(username, displayName, passwordHash, role, active, occurredAt, actorUserId);
    }

    public boolean update(
            String newDisplayName,
            Role newRole,
            Boolean newActive,
            Instant occurredAt,
            Long actorUserId) {
        boolean securityChanged = false;
        if (newDisplayName != null) {
            this.displayName = requiredText(newDisplayName, "displayName");
        }
        if (newRole != null && newRole != this.role) {
            this.role = newRole;
            securityChanged = true;
        }
        if (newActive != null && newActive != this.active) {
            this.active = newActive;
            securityChanged = true;
        }
        if (securityChanged) {
            this.authVersion++;
        }
        this.updatedAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.updatedBy = Objects.requireNonNull(actorUserId, "actorUserId is required");
        return securityChanged;
    }

    public void changePassword(String newPasswordHash, Instant occurredAt, Long actorUserId) {
        this.passwordHash = requiredText(newPasswordHash, "newPasswordHash");
        this.authVersion++;
        this.updatedAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.updatedBy = Objects.requireNonNull(actorUserId, "actorUserId is required");
    }

    private static String requiredText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field + " is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String usernameNormalized() {
        return usernameNormalized;
    }

    public String displayName() {
        return displayName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public long authVersion() {
        return authVersion;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Long createdBy() {
        return createdBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long updatedBy() {
        return updatedBy;
    }
}
