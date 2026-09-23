package com.rrjaggery.production.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecipeDto {
    private UUID id;
    private String recipeCode;
    private String recipeName;
    private String outputProductSku;
    private String outputProductName;
    private BigDecimal standardBatchSize;
    private String unitOfMeasure;
    private BigDecimal standardYieldPercentage;
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RecipeItemDto> items = new ArrayList<>();

    public RecipeDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getOutputProductSku() {
        return outputProductSku;
    }

    public void setOutputProductSku(String outputProductSku) {
        this.outputProductSku = outputProductSku;
    }

    public String getOutputProductName() {
        return outputProductName;
    }

    public void setOutputProductName(String outputProductName) {
        this.outputProductName = outputProductName;
    }

    public BigDecimal getStandardBatchSize() {
        return standardBatchSize;
    }

    public void setStandardBatchSize(BigDecimal standardBatchSize) {
        this.standardBatchSize = standardBatchSize;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getStandardYieldPercentage() {
        return standardYieldPercentage;
    }

    public void setStandardYieldPercentage(BigDecimal standardYieldPercentage) {
        this.standardYieldPercentage = standardYieldPercentage;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<RecipeItemDto> getItems() {
        return items;
    }

    public void setItems(List<RecipeItemDto> items) {
        this.items = items;
    }
}
