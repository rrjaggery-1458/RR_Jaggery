package com.rrjaggery.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class RecipeItemDto {
    private UUID id;
    private String rawMaterialSku;
    private String rawMaterialName;
    private BigDecimal requiredQuantity;
    private String unitOfMeasure;
    private String notes;

    public RecipeItemDto() {
    }

    public RecipeItemDto(UUID id, String rawMaterialSku, String rawMaterialName, BigDecimal requiredQuantity, String unitOfMeasure, String notes) {
        this.id = id;
        this.rawMaterialSku = rawMaterialSku;
        this.rawMaterialName = rawMaterialName;
        this.requiredQuantity = requiredQuantity;
        this.unitOfMeasure = unitOfMeasure;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
