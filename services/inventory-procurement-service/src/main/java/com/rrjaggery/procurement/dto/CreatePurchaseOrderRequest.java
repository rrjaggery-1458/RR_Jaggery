package com.rrjaggery.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class CreatePurchaseOrderRequest {
    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @NotBlank(message = "Expected delivery date is required")
    private String expectedDeliveryDate;

    private String notes;

    @NotEmpty(message = "At least one purchase order line is required")
    private List<PurchaseOrderLineRequest> lines;

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public String getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(String expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PurchaseOrderLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLineRequest> lines) {
        this.lines = lines;
    }

    public static class PurchaseOrderLineRequest {
        @NotBlank(message = "SKU is required")
        private String sku;

        @NotBlank(message = "Item name is required")
        private String itemName;

        @NotNull(message = "Ordered quantity is required")
        private java.math.BigDecimal orderedQuantity;

        @NotNull(message = "Unit cost is required")
        private java.math.BigDecimal unitCost;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public java.math.BigDecimal getOrderedQuantity() {
            return orderedQuantity;
        }

        public void setOrderedQuantity(java.math.BigDecimal orderedQuantity) {
            this.orderedQuantity = orderedQuantity;
        }

        public java.math.BigDecimal getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(java.math.BigDecimal unitCost) {
            this.unitCost = unitCost;
        }
    }
}
