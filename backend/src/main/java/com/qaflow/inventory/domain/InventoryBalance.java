package com.qaflow.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.qaflow.material.domain.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_balances")
public class InventoryBalance {

    public static final BigDecimal MAX_QUANTITY = new BigDecimal("9999999999999.999999");

    @Id
    @Column(name = "material_id")
    private Long materialId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private Material material;

    @Column(name = "on_hand_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal reservedQuantity;

    @Column(name = "available_quantity", insertable = false, updatable = false, precision = 19, scale = 6)
    private BigDecimal availableQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    protected InventoryBalance() {
    }

    public void apply(InventoryMovementType type, BigDecimal quantity, Instant occurredAt, long actorUserId) {
        Objects.requireNonNull(type, "type is required");
        BigDecimal exactQuantity = requirePositiveExact(quantity);
        if (type.direction() == InventoryDirection.IN) {
            BigDecimal next = onHandQuantity.add(exactQuantity);
            if (next.compareTo(MAX_QUANTITY) > 0) {
                throw new QuantityOverflowException();
            }
            onHandQuantity = next;
        } else {
            if (availableQuantity().compareTo(exactQuantity) < 0) {
                throw new InsufficientStockException();
            }
            onHandQuantity = onHandQuantity.subtract(exactQuantity);
        }
        updatedAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        updatedBy = actorUserId;
    }

    private BigDecimal requirePositiveExact(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "quantity is required");
        if (quantity.scale() != 6 || quantity.signum() <= 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new IllegalArgumentException("quantity must be positive DECIMAL(19,6)");
        }
        return quantity;
    }

    public Long materialId() {
        return materialId;
    }

    public Material material() {
        return material;
    }

    public BigDecimal onHandQuantity() {
        return onHandQuantity;
    }

    public BigDecimal reservedQuantity() {
        return reservedQuantity;
    }

    public BigDecimal availableQuantity() {
        return onHandQuantity.subtract(reservedQuantity);
    }

    public long version() {
        return version;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long updatedBy() {
        return updatedBy;
    }

    public static final class InsufficientStockException extends RuntimeException {
        public InsufficientStockException() {
            super("Insufficient available inventory");
        }
    }

    public static final class QuantityOverflowException extends RuntimeException {
        public QuantityOverflowException() {
            super("Inventory quantity exceeds DECIMAL(19,6)");
        }
    }
}
