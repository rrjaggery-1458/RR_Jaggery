package com.rrjaggery.finance.dto;

import java.math.BigDecimal;

public class StockValuationItemDto {
    private String id;
    private String sku;
    private String itemName;
    private String itemType; // FINISHED_GOOD, RAW_MATERIAL
    private BigDecimal currentQuantity;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal stockValue;
    private String valuationStatus; // "AUTHORITATIVE", "VALUATION_UNAVAILABLE"

    public StockValuationItemDto() {}

    public StockValuationItemDto(String id, String sku, String itemName, String itemType,
                                 BigDecimal currentQuantity, String unitOfMeasure,
                                 BigDecimal unitPrice, BigDecimal stockValue, String valuationStatus) {
        this.id = id;
        this.sku = sku;
        this.itemName = itemName;
        this.itemType = itemType;
        this.currentQuantity = currentQuantity;
        this.unitOfMeasure = unitOfMeasure;
        this.unitPrice = unitPrice;
        this.stockValue = stockValue;
        this.valuationStatus = valuationStatus;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public BigDecimal getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(BigDecimal currentQuantity) { this.currentQuantity = currentQuantity; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getStockValue() { return stockValue; }
    public void setStockValue(BigDecimal stockValue) { this.stockValue = stockValue; }

    public String getValuationStatus() { return valuationStatus; }
    public void setValuationStatus(String valuationStatus) { this.valuationStatus = valuationStatus; }
}
