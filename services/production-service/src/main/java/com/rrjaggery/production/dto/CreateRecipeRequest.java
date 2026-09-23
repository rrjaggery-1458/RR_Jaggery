package com.rrjaggery.production.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CreateRecipeRequest {

    @NotBlank(message = "Recipe code is required")
    private String recipeCode;

    @NotBlank(message = "Recipe name is required")
    private String recipeName;

    @NotBlank(message = "Output product SKU is required")
    private String outputProductSku;

    @NotBlank(message = "Output product name is required")
    private String outputProductName;

    @NotNull(message = "Standard batch size is required")
    @DecimalMin(value = "0.001", inclusive = false, message = "Standard batch size must be greater than 0")
    private BigDecimal standardBatchSize;

    private String unitOfMeasure = "KG";

    private BigDecimal standardYieldPercentage;

    private Integer estimatedDurationMinutes;

    private String description;

    @NotEmpty(message = "At least one recipe ingredient/item is required")
    @Valid
    private List<RecipeItemRequest> items = new ArrayList<>();

    public CreateRecipeRequest() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RecipeItemRequest> getItems() {
        return items;
    }

    public void setItems(List<RecipeItemRequest> items) {
        this.items = items;
    }
}
