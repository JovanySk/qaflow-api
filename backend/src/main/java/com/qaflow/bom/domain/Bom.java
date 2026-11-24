package com.qaflow.bom.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.qaflow.material.domain.Material;
import com.qaflow.product.domain.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "boms")
public class Bom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "current_product_id", insertable = false, updatable = false)
    private Long currentProductId;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "superseded_at", columnDefinition = "datetime(6)")
    private Instant supersededAt;

    @Column(name = "superseded_by")
    private Long supersededBy;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.PERSIST, orphanRemoval = false)
    @OrderBy("id ASC")
    private List<BomItem> items = new ArrayList<>();

    protected Bom() {
    }

    private Bom(Product product, int revisionNumber, Instant createdAt, long createdBy) {
        this.product = Objects.requireNonNull(product, "product is required");
        if (revisionNumber <= 0) {
            throw new IllegalArgumentException("revisionNumber must be positive");
        }
        this.revisionNumber = revisionNumber;
        this.current = true;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.createdBy = createdBy;
    }

    public static Bom create(Product product, int revisionNumber, Instant createdAt, long createdBy) {
        return new Bom(product, revisionNumber, createdAt, createdBy);
    }

    public void addItem(Material material, BigDecimal quantityPerUnit, Instant createdAt) {
        Objects.requireNonNull(material, "material is required");
        if (items.stream().anyMatch(item -> Objects.equals(item.material().id(), material.id()))) {
            throw new IllegalArgumentException("material must not be duplicated in a BOM");
        }
        items.add(BomItem.create(this, material, quantityPerUnit, createdAt));
    }

    public void supersede(Instant occurredAt, long actorUserId) {
        if (!current) {
            throw new IllegalStateException("only the current BOM can be superseded");
        }
        current = false;
        supersededAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        supersededBy = actorUserId;
    }

    public Long id() {
        return id;
    }

    public Product product() {
        return product;
    }

    public int revisionNumber() {
        return revisionNumber;
    }

    public boolean current() {
        return current;
    }

    public Long currentProductId() {
        return currentProductId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Long createdBy() {
        return createdBy;
    }

    public Instant supersededAt() {
        return supersededAt;
    }

    public Long supersededBy() {
        return supersededBy;
    }

    public List<BomItem> items() {
        return Collections.unmodifiableList(items);
    }
}
