package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "production_batches", schema = "production_schema",
        uniqueConstraints = @UniqueConstraint(name = "uk_batch_number", columnNames = "batch_number"),
        indexes = {
                @Index(name = "idx_batch_status", columnList = "status"),
                @Index(name = "idx_batch_target_sku", columnList = "target_product_sku"),
                @Index(name = "idx_batch_lot", columnList = "batch_lot")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_number", nullable = false, length = 80)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "target_product_sku", nullable = false, length = 100)
    private String targetProductSku;

    @Column(name = "target_product_name", nullable = false, length = 180)
    private String targetProductName;

    @Column(name = "planned_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal plannedQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal actualQuantity = BigDecimal.ZERO;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BatchStatus status = BatchStatus.PLANNED;

    @Column(name = "planned_start_date")
    private Instant plannedStartDate;

    @Column(name = "actual_start_date")
    private Instant actualStartDate;

    @Column(name = "completion_date")
    private Instant completionDate;

    @Column(name = "supervisor", length = 120)
    private String supervisor;

    @Column(name = "batch_lot", length = 100)
    private String batchLot;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "total_wastage_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal totalWastageQuantity = BigDecimal.ZERO;

    @Column(name = "yield_percentage", precision = 7, scale = 2)
    private BigDecimal yieldPercentage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchConsumption> consumptions = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchOutput> outputs = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchWastage> wastages = new ArrayList<>();

    public ProductionBatch() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getTargetProductSku() {
        return targetProductSku;
    }

    public void setTargetProductSku(String targetProductSku) {
        this.targetProductSku = targetProductSku;
    }

    public String getTargetProductName() {
        return targetProductName;
    }

    public void setTargetProductName(String targetProductName) {
        this.targetProductName = targetProductName;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(BigDecimal plannedQuantity) {
        this.plannedQuantity = plannedQuantity == null ? BigDecimal.ZERO : plannedQuantity;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity == null ? BigDecimal.ZERO : actualQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Instant getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(Instant plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public Instant getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(Instant actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public Instant getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Instant completionDate) {
        this.completionDate = completionDate;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getBatchLot() {
        return batchLot;
    }

    public void setBatchLot(String batchLot) {
        this.batchLot = batchLot;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getTotalWastageQuantity() {
        return totalWastageQuantity;
    }

    public void setTotalWastageQuantity(BigDecimal totalWastageQuantity) {
        this.totalWastageQuantity = totalWastageQuantity == null ? BigDecimal.ZERO : totalWastageQuantity;
    }

    public BigDecimal getYieldPercentage() {
        return yieldPercentage;
    }

    public void setYieldPercentage(BigDecimal yieldPercentage) {
        this.yieldPercentage = yieldPercentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BatchConsumption> getConsumptions() {
        return consumptions;
    }

    public void setConsumptions(List<BatchConsumption> consumptions) {
        this.consumptions = consumptions == null ? new ArrayList<>() : consumptions;
    }

    public List<BatchOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<BatchOutput> outputs) {
        this.outputs = outputs == null ? new ArrayList<>() : outputs;
    }

    public List<BatchWastage> getWastages() {
        return wastages;
    }

    public void setWastages(List<BatchWastage> wastages) {
        this.wastages = wastages == null ? new ArrayList<>() : wastages;
    }
}
