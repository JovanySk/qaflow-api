package com.qaflow.material.domain;

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
@Table(name = "materials")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "code_normalized", insertable = false, updatable = false, length = 64)
    private String codeNormalized;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 32)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    protected Material() {
    }

    private Material(
            String code,
            String name,
            String description,
            UnitOfMeasure unitOfMeasure,
            boolean active,
            Instant occurredAt,
            long actorUserId) {
        this.code = requiredText(code, "code");
        this.name = requiredText(name, "name");
        this.description = optionalText(description);
        this.unitOfMeasure = Objects.requireNonNull(unitOfMeasure, "unitOfMeasure is required");
        this.active = active;
        this.version = 0;
        this.createdAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.createdBy = actorUserId;
        this.updatedAt = occurredAt;
        this.updatedBy = actorUserId;
    }

    public static Material create(
            String code,
            String name,
            String description,
            UnitOfMeasure unitOfMeasure,
            boolean active,
            Instant occurredAt,
            long actorUserId) {
        return new Material(code, name, description, unitOfMeasure, active, occurredAt, actorUserId);
    }

    public void changeCode(String newCode) {
        this.code = requiredText(newCode, "code");
    }

    public void changeName(String newName) {
        this.name = requiredText(newName, "name");
    }

    public void changeDescription(String newDescription) {
        this.description = optionalText(newDescription);
    }

    public void changeUnitOfMeasure(UnitOfMeasure newUnitOfMeasure) {
        this.unitOfMeasure = Objects.requireNonNull(newUnitOfMeasure, "unitOfMeasure is required");
    }

    public void changeActive(boolean newActive) {
        this.active = newActive;
    }

    public void markUpdated(Instant occurredAt, long actorUserId) {
        this.updatedAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.updatedBy = actorUserId;
    }

    private static String requiredText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field + " is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String optionalText(String value) {
        return value == null ? null : value.trim();
    }

    public Long id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String codeNormalized() {
        return codeNormalized;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public UnitOfMeasure unitOfMeasure() {
        return unitOfMeasure;
    }

    public boolean active() {
        return active;
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
