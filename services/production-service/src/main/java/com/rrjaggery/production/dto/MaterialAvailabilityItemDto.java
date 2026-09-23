package com.rrjaggery.production.dto;

import java.math.BigDecimal;

public class MaterialAvailabilityItemDto {
    private String rawMaterialSku;
    private String rawMaterialName;
    private BigDecimal requiredQuantity;
    private BigDecimal currentStock;
    private BigDecimal allocatedStock;
    private BigDecimal availableStock;
    private String unitOfMeasure;
    private boolean isSufficient;
    private BigDecimal shortfall;

    public MaterialAvailabilityItemDto() {
    }

    public MaterialAvailabilityItemDto(String rawMaterialSku, String rawMaterialName, BigDecimal requiredQuantity,
                                      BigDecimal currentStock, BigDecimal allocatedStock, BigDecimal availableStock,
                                      String unitOfMeasure, boolean isSufficient, BigDecimal shortfall) {
        this.rawMaterialSku = rawMaterialSku;
        this.rawMaterialName = rawMaterialName;
        this.requiredQuantity = requiredQuantity;
        this.currentStock = currentStock;
        this.allocatedStock = allocatedStock;
        this.availableStock = availableStock;
        this.unitOfMeasure = unitOfMeasure;
        this.isSufficient = isSufficient;
        this.shortfall = shortfall;
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

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getAllocatedStock() {
        return allocatedStock;
    }

    public void setAllocatedStock(BigDecimal allocatedStock) {
        this.allocatedStock = allocatedStock;
    }

    public BigDecimal getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(BigDecimal availableStock) {
        this.availableStock = availableStock;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public boolean isSufficient() {
        return isSufficient;
    }

    public void setSufficient(boolean sufficient) {
        isSufficient = sufficient;
    }

    public BigDecimal getShortfall() {
        return shortfall;
    }

    public void setShortfall(BigDecimal shortfall) {
        this.shortfall = shortfall;
    }
}
