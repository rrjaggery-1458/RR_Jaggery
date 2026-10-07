package com.rrjaggery.production.dto;

import com.rrjaggery.production.entity.BatchStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreateBatchRequest {

    private UUID recipeId;
    private String recipeCode;

    @NotBlank(message = "Target product SKU is required")
    private String targetProductSku;

    private String targetProductName;

    @DecimalMin(value = "0.001", inclusive = false, message = "Planned quantity must be greater than 0")
    private BigDecimal plannedQuantity;

    private BigDecimal actualQuantity;
    private BigDecimal wastageQuantity;

    private BatchStatus status = BatchStatus.PLANNED;

    private String unitOfMeasure = "KG";

    private Instant plannedStartDate;
    private String scheduledDate;

    private String supervisor;

    private String notes;

    public CreateBatchRequest() {
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(UUID recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getTargetProductSku() {
        return targetProductSku;
    }

    public void setTargetProductSku(String targetProductSku) {
        this.targetProductSku = targetProductSku;
    }

    public void setOutputSku(String outputSku) {
        if (this.targetProductSku == null) {
            this.targetProductSku = outputSku;
        }
    }

    public void setProductSku(String productSku) {
        if (this.targetProductSku == null) {
            this.targetProductSku = productSku;
        }
    }

    public void setJaggeryType(String jaggeryType) {
        if (this.targetProductSku == null) {
            this.targetProductSku = jaggeryType;
        }
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
        this.plannedQuantity = plannedQuantity;
    }

    public void setQuantityKg(BigDecimal qty) {
        if (this.plannedQuantity == null) {
            this.plannedQuantity = qty;
        }
        if (this.actualQuantity == null) {
            this.actualQuantity = qty;
        }
    }

    // Alias for plannedQuantityKg
    public BigDecimal getPlannedQuantityKg() {
        return plannedQuantity;
    }

    public void setPlannedQuantityKg(BigDecimal plannedQuantityKg) {
        if (this.plannedQuantity == null) {
            this.plannedQuantity = plannedQuantityKg;
        }
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public BigDecimal getActualQuantityKg() {
        return actualQuantity;
    }

    public void setActualQuantityKg(BigDecimal actualQuantityKg) {
        if (this.actualQuantity == null) {
            this.actualQuantity = actualQuantityKg;
        }
    }

    public BigDecimal getWastageQuantity() {
        return wastageQuantity;
    }

    public void setWastageQuantity(BigDecimal wastageQuantity) {
        this.wastageQuantity = wastageQuantity;
    }

    public BigDecimal getWastageQuantityKg() {
        return wastageQuantity;
    }

    public void setWastageQuantityKg(BigDecimal wastageQuantityKg) {
        if (this.wastageQuantity == null) {
            this.wastageQuantity = wastageQuantityKg;
        }
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Instant getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(Instant plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(String scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

