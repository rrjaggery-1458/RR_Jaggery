package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_consumptions", schema = "production_schema",
        indexes = {
                @Index(name = "idx_consumption_batch", columnList = "batch_id"),
                @Index(name = "idx_consumption_sku", columnList = "raw_material_sku"),
                @Index(name = "idx_consumption_idempotency", columnList = "idempotency_key", unique = true)
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BatchConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductionBatch batch;

    @Column(name = "raw_material_sku", nullable = false, length = 100)
    private String rawMaterialSku;

    @Column(name = "raw_material_name", nullable = false, length = 180)
    private String rawMaterialName;

    @Column(name = "planned_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;

    @Column(name = "consumed_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal consumedQuantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt = Instant.now();

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    public BatchConsumption() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductionBatch batch) {
        this.batch = batch;
    }

    public String getRawMaterialSku() {
        return rawMaterialSku;
    }

    public void setRawMaterialSku(String rawMaterialSku) {
        this.rawMaterialSku = rawMaterialSku;
    }

    public String getRawMaterialName() {
        return rawMaterialName;
    }

    public void setRawMaterialName(String rawMaterialName) {
        this.rawMaterialName = rawMaterialName;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(BigDecimal plannedQuantity) {
        this.plannedQuantity = plannedQuantity == null ? BigDecimal.ZERO : plannedQuantity;
    }

    public BigDecimal getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(BigDecimal consumedQuantity) {
        this.consumedQuantity = consumedQuantity == null ? BigDecimal.ZERO : consumedQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
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
}
