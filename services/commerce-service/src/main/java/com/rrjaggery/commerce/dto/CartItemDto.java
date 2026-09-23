package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.CartItem;
import com.rrjaggery.commerce.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;

public class CartItemDto {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private String grade;
    private String packageType;
    private BigDecimal unitWeightKg;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private String imageUrl;
    private boolean active;

    public CartItemDto() {}

    public static CartItemDto fromEntity(CartItem item, BigDecimal applicablePrice) {
        CartItemDto dto = new CartItemDto();
        dto.id = item.getId();
        Product p = item.getProduct();
        dto.productId = p.getId();
        dto.productName = p.getName();
        dto.sku = p.getSku();
        dto.grade = p.getGrade().name();
        dto.packageType = p.getPackageType().name();
        dto.unitWeightKg = p.getUnitWeightKg();
        dto.unitPrice = applicablePrice;
        dto.quantity = item.getQuantity();
        dto.lineTotal = applicablePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        dto.imageUrl = p.getImageUrl();
        dto.active = p.isActive();
        return dto;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getGrade() { return grade; }
    public String getPackageType() { return packageType; }
    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
}
