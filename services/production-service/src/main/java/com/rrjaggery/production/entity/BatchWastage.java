package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_wastages", schema = "production_schema",
        indexes = {
                @Index(name = "idx_wastage_batch", columnList = "batch_id"),
                @Index(name = "idx_wastage_sku", columnList = "material_sku"),
                @Index(name = "idx_wastage_idempotency", columnList = "idempotency_key", unique = true)
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BatchWastage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductionBatch batch;

    @Column(name = "material_sku", nullable = false, length = 100)
    private String materialSku;

    @Column(name = "material_name", nullable = false, length = 180)
    private String materialName;

    @Column(name = "wastage_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal wastageQuantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "wastage_type", length = 50)
    private String wastageType = "SCRAP"; // SCRAP, LOSS, SPILLAGE, DEFECT

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    public BatchWastage() {
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

    public String getMaterialSku() {
        return materialSku;
    }

    public void setMaterialSku(String materialSku) {
        this.materialSku = materialSku;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public BigDecimal getWastageQuantity() {
        return wastageQuantity;
    }

    public void setWastageQuantity(BigDecimal wastageQuantity) {
        this.wastageQuantity = wastageQuantity == null ? BigDecimal.ZERO : wastageQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getWastageType() {
        return wastageType;
    }

    public void setWastageType(String wastageType) {
        this.wastageType = wastageType == null || wastageType.isBlank() ? "SCRAP" : wastageType;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
