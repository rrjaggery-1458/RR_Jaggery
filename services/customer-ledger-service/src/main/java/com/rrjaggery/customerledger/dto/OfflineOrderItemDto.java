package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.OfflineOrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public class OfflineOrderItemDto {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal unitWeightKg;
    private BigDecimal lineTotal;
    private BigDecimal taxAmount;

    public OfflineOrderItemDto() {}

    public static OfflineOrderItemDto fromEntity(OfflineOrderItem item) {
        OfflineOrderItemDto dto = new OfflineOrderItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setSku(item.getSku());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setUnitWeightKg(item.getUnitWeightKg());
        dto.setLineTotal(item.getLineTotal());
        dto.setTaxAmount(item.getTaxAmount());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
}
