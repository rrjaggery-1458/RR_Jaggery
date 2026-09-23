package com.rrjaggery.customerledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class OfflineOrderItemRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Product Name is required")
    private String productName;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotNull(message = "Unit wholesale price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be positive")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Unit weight in kg is required")
    @DecimalMin(value = "0.001", message = "Weight must be positive")
    private BigDecimal unitWeightKg;

    public OfflineOrderItemRequest() {}

    public OfflineOrderItemRequest(UUID productId, String productName, String sku,
                                   BigDecimal unitPrice, int quantity, BigDecimal unitWeightKg) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.unitWeightKg = unitWeightKg;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public void setUnitWeightKg(BigDecimal unitWeightKg) { this.unitWeightKg = unitWeightKg; }
}
