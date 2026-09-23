package com.rrjaggery.production.dto;

import com.rrjaggery.production.entity.BatchConsumption;
import com.rrjaggery.production.entity.BatchOutput;
import com.rrjaggery.production.entity.BatchStatus;
import com.rrjaggery.production.entity.BatchWastage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductionBatchDto {
    private UUID id;
    private String batchNumber;
    private UUID recipeId;
    private String recipeCode;
    private String recipeName;
    private String targetProductSku;
    private String targetProductName;
    private BigDecimal plannedQuantity;
    private BigDecimal actualQuantity;
    private String unitOfMeasure;
    private BatchStatus status;
    private Instant plannedStartDate;
    private Instant actualStartDate;
    private Instant completionDate;
    private String supervisor;
    private String batchLot;
    private String notes;
    private BigDecimal totalWastageQuantity;
    private BigDecimal yieldPercentage;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BatchConsumption> consumptions = new ArrayList<>();
    private List<BatchOutput> outputs = new ArrayList<>();
    private List<BatchWastage> wastages = new ArrayList<>();

    public ProductionBatchDto() {
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

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
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
        this.plannedQuantity = plannedQuantity;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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
        this.totalWastageQuantity = totalWastageQuantity;
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
        this.consumptions = consumptions;
    }

    public List<BatchOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<BatchOutput> outputs) {
        this.outputs = outputs;
    }

    public List<BatchWastage> getWastages() {
        return wastages;
    }

    public void setWastages(List<BatchWastage> wastages) {
        this.wastages = wastages;
    }
}
