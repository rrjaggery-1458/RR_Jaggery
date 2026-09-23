package com.rrjaggery.inventory.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", schema = "inventory_schema",
        indexes = {
                @Index(name = "idx_stock_movement_item", columnList = "item_id"),
                @Index(name = "idx_stock_movement_reference", columnList = "reference_id"),
                @Index(name = "idx_stock_movement_idempotency", columnList = "idempotency_key", unique = true)
        })
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "movement_time", nullable = false)
    private Instant movementTime = Instant.now();

    public StockMovement() {
    }

    public StockMovement(InventoryItem item, MovementType movementType, BigDecimal quantity,
                        String reason, String referenceType, String referenceId, String actor, String notes,
                        String idempotencyKey) {
        this.item = item;
        this.movementType = movementType;
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
        this.reason = reason;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.actor = actor;
        this.notes = notes;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InventoryItem getItem() {
        return item;
    }

    public void setItem(InventoryItem item) {
        this.item = item;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getMovementTime() {
        return movementTime;
    }

    public void setMovementTime(Instant movementTime) {
        this.movementTime = movementTime;
    }
}
