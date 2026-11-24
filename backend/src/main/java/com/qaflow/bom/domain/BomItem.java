package com.qaflow.bom.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.qaflow.material.domain.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bom_items")
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "quantity_per_unit", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityPerUnit;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    protected BomItem() {
    }

    private BomItem(Bom bom, Material material, BigDecimal quantityPerUnit, Instant createdAt) {
        this.bom = Objects.requireNonNull(bom, "bom is required");
        this.material = Objects.requireNonNull(material, "material is required");
        this.quantityPerUnit = Objects.requireNonNull(quantityPerUnit, "quantityPerUnit is required");
        if (quantityPerUnit.signum() <= 0 || quantityPerUnit.scale() != 6 || quantityPerUnit.precision() > 19) {
            throw new IllegalArgumentException("quantityPerUnit must be a positive DECIMAL(19,6)");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    static BomItem create(Bom bom, Material material, BigDecimal quantityPerUnit, Instant createdAt) {
        return new BomItem(bom, material, quantityPerUnit, createdAt);
    }

    public Long id() {
        return id;
    }

    public Bom bom() {
        return bom;
    }

    public Material material() {
        return material;
    }

    public BigDecimal quantityPerUnit() {
        return quantityPerUnit;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
