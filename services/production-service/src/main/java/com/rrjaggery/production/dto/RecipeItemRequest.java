package com.rrjaggery.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RecipeItemRequest {

    @NotBlank(message = "Raw material SKU is required")
    private String rawMaterialSku;

    @NotBlank(message = "Raw material name is required")
    private String rawMaterialName;

    @NotNull(message = "Required quantity is required")
    @DecimalMin(value = "0.0001", inclusive = false, message = "Required quantity must be greater than 0")
    private BigDecimal requiredQuantity;

    private String unitOfMeasure = "KG";

    private String notes;

    public RecipeItemRequest() {
    }

    public RecipeItemRequest(String rawMaterialSku, String rawMaterialName, BigDecimal requiredQuantity, String unitOfMeasure) {
        this.rawMaterialSku = rawMaterialSku;
        this.rawMaterialName = rawMaterialName;
        this.requiredQuantity = requiredQuantity;
        this.unitOfMeasure = unitOfMeasure;
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

    public BigDecimal getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(BigDecimal requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
