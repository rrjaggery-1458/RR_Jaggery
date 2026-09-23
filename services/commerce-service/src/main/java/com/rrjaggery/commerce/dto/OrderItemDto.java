package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemDto {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal unitWeightKg;
    private BigDecimal lineTotal;
    private BigDecimal taxAmount;

    public OrderItemDto() {}

    public static OrderItemDto fromEntity(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.id = item.getId();
        dto.productId = item.getProductId();
        dto.productName = item.getProductName();
        dto.sku = item.getSku();
        dto.unitPrice = item.getUnitPrice();
        dto.quantity = item.getQuantity();
        dto.unitWeightKg = item.getUnitWeightKg();
        dto.lineTotal = item.getLineTotal();
        dto.taxAmount = item.getTaxAmount();
        return dto;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
}
