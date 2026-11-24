package com.qaflow.product.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "sku_normalized", insertable = false, updatable = false, length = 64)
    private String skuNormalized;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

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

    protected Product() {
    }

    private Product(
            String sku,
            String name,
            String description,
            boolean active,
            Instant occurredAt,
            long actorUserId) {
        this.sku = requiredText(sku, "sku");
        this.name = requiredText(name, "name");
        this.description = optionalText(description);
        this.active = active;
        this.version = 0;
        this.createdAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.createdBy = actorUserId;
        this.updatedAt = occurredAt;
        this.updatedBy = actorUserId;
    }

    public static Product create(
            String sku,
            String name,
            String description,
            boolean active,
            Instant occurredAt,
            long actorUserId) {
        return new Product(sku, name, description, active, occurredAt, actorUserId);
    }

    public void changeSku(String newSku) {
        this.sku = requiredText(newSku, "sku");
    }

    public void changeName(String newName) {
        this.name = requiredText(newName, "name");
    }

    public void changeDescription(String newDescription) {
        this.description = optionalText(newDescription);
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

    public String sku() {
        return sku;
    }

    public String skuNormalized() {
        return skuNormalized;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
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
