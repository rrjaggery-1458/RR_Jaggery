package com.rrjaggery.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreateBatchRequest {

    private UUID recipeId;
    private String recipeCode;

    @NotBlank(message = "Target product SKU is required")
    private String targetProductSku;

    private String targetProductName;

    @NotNull(message = "Planned quantity is required")
    @DecimalMin(value = "0.001", inclusive = false, message = "Planned quantity must be greater than 0")
    private BigDecimal plannedQuantity;

    private String unitOfMeasure = "KG";

    private Instant plannedStartDate;

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
