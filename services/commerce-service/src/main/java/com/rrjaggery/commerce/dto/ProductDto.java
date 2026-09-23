package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductGrade;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductDto {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String sku;
    private String slug;
    private String description;
    private ProductGrade grade;
    private PackageType packageType;
    private BigDecimal unitWeightKg;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private int wholesaleMoq;
    private String imageUrl;
    private boolean featured;
    private boolean active;

    public ProductDto() {}

    public static ProductDto fromEntity(Product product, String categoryName) {
        ProductDto dto = new ProductDto();
        dto.id = product.getId();
        dto.categoryId = product.getCategoryId();
        dto.categoryName = categoryName;
        dto.name = product.getName();
        dto.sku = product.getSku();
        dto.slug = product.getSlug();
        dto.description = product.getDescription();
        dto.grade = product.getGrade();
        dto.packageType = product.getPackageType();
        dto.unitWeightKg = product.getUnitWeightKg();
        dto.retailPrice = product.getRetailPrice();
        dto.wholesalePrice = product.getWholesalePrice();
        dto.wholesaleMoq = product.getWholesaleMoq();
        dto.imageUrl = product.getImageUrl();
        dto.featured = product.isFeatured();
        dto.active = product.isActive();
        return dto;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public ProductGrade getGrade() { return grade; }
    public PackageType getPackageType() { return packageType; }
    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public BigDecimal getRetailPrice() { return retailPrice; }
    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public int getWholesaleMoq() { return wholesaleMoq; }
    public String getImageUrl() { return imageUrl; }
    public boolean featured() { return featured; }
    public boolean isActive() { return active; }
}
