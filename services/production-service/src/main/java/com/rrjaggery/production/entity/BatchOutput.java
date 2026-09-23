package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_outputs", schema = "production_schema",
        indexes = {
                @Index(name = "idx_output_batch", columnList = "batch_id"),
                @Index(name = "idx_output_sku", columnList = "finished_good_sku"),
                @Index(name = "idx_output_lot", columnList = "batch_lot"),
                @Index(name = "idx_output_idempotency", columnList = "idempotency_key", unique = true)
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BatchOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductionBatch batch;

    @Column(name = "finished_good_sku", nullable = false, length = 100)
    private String finishedGoodSku;

    @Column(name = "finished_good_name", nullable = false, length = 180)
    private String finishedGoodName;

    @Column(name = "quantity_produced", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityProduced;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Column(name = "batch_lot", length = 100)
    private String batchLot;

    @Column(name = "quality_grade", length = 50)
    private String qualityGrade = "A_GRADE"; // A_GRADE, B_GRADE, PREMIUM

    @Column(name = "produced_at", nullable = false)
    private Instant producedAt = Instant.now();

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    public BatchOutput() {
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

    public String getFinishedGoodSku() {
        return finishedGoodSku;
    }

    public void setFinishedGoodSku(String finishedGoodSku) {
        this.finishedGoodSku = finishedGoodSku;
    }

    public String getOutputProductSku() {
        return finishedGoodSku;
    }

    public void setOutputProductSku(String outputProductSku) {
        this.finishedGoodSku = outputProductSku;
    }

    public String getFinishedGoodName() {
        return finishedGoodName;
    }

    public void setFinishedGoodName(String finishedGoodName) {
        this.finishedGoodName = finishedGoodName;
    }

    public String getOutputProductName() {
        return finishedGoodName;
    }

    public void setOutputProductName(String outputProductName) {
        this.finishedGoodName = outputProductName;
    }

    public BigDecimal getQuantityProduced() {
        return quantityProduced;
    }

    public void setQuantityProduced(BigDecimal quantityProduced) {
        this.quantityProduced = quantityProduced == null ? BigDecimal.ZERO : quantityProduced;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public String getBatchLot() {
        return batchLot;
    }

    public void setBatchLot(String batchLot) {
        this.batchLot = batchLot;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade != null ? qualityGrade : "A_GRADE";
    }

    public Instant getProducedAt() {
        return producedAt;
    }

    public void setProducedAt(Instant producedAt) {
        this.producedAt = producedAt;
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
