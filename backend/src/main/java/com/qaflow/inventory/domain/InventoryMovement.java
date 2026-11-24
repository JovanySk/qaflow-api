package com.qaflow.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.qaflow.material.domain.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 32, updatable = false)
    private InventoryMovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 3, updatable = false)
    private InventoryDirection direction;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantity;

    @Column(name = "on_hand_after", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal onHandAfter;

    @Column(name = "reserved_after", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal reservedAfter;

    @Column(name = "production_order_id", updatable = false)
    private Long productionOrderId;

    @Column(name = "reference_code", length = 120, updatable = false)
    private String referenceCode;

    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    @Column(name = "performed_by", nullable = false, updatable = false)
    private Long performedBy;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "datetime(6)", updatable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, length = 64, updatable = false)
    private String correlationId;

    protected InventoryMovement() {
    }

    public static InventoryMovement manual(
            Material material,
            InventoryMovementType movementType,
            BigDecimal quantity,
            InventoryBalance balance,
            String referenceCode,
            String reason,
            long actorUserId,
            Instant occurredAt,
            String correlationId) {
        if (!Objects.requireNonNull(movementType, "movementType is required").manual()) {
            throw new IllegalArgumentException("Production consumption is not a manual movement");
        }
        InventoryMovement movement = new InventoryMovement();
        movement.material = Objects.requireNonNull(material, "material is required");
        movement.movementType = movementType;
        movement.direction = movementType.direction();
        movement.quantity = Objects.requireNonNull(quantity, "quantity is required");
        movement.onHandAfter = balance.onHandQuantity();
        movement.reservedAfter = balance.reservedQuantity();
        movement.productionOrderId = null;
        movement.referenceCode = optionalText(referenceCode);
        movement.reason = optionalText(reason);
        movement.performedBy = actorUserId;
        movement.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        movement.correlationId = requiredText(correlationId, "correlationId");
        return movement;
    }

    private static String optionalText(String value) {
        return value == null ? null : value.trim();
    }

    private static String requiredText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field + " is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    public Long id() { return id; }
    public Material material() { return material; }
    public InventoryMovementType movementType() { return movementType; }
    public InventoryDirection direction() { return direction; }
    public BigDecimal quantity() { return quantity; }
    public BigDecimal onHandAfter() { return onHandAfter; }
    public BigDecimal reservedAfter() { return reservedAfter; }
    public Long productionOrderId() { return productionOrderId; }
    public String referenceCode() { return referenceCode; }
    public String reason() { return reason; }
    public Long performedBy() { return performedBy; }
    public Instant occurredAt() { return occurredAt; }
    public String correlationId() { return correlationId; }
}
