package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.ProductGrade;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateProductRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String slug;
    private String description;

    @NotNull(message = "Grade is required")
    private ProductGrade grade = ProductGrade.GRADE_A_TRADITIONAL;

    @NotNull(message = "Package type is required")
    private PackageType packageType = PackageType.BOX;

    @NotNull(message = "Unit weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be positive")
    private BigDecimal unitWeightKg = BigDecimal.ONE;

    @NotNull(message = "Retail price is required")
    @DecimalMin(value = "0.01", message = "Retail price must be positive")
    private BigDecimal retailPrice;

    @NotNull(message = "Wholesale price is required")
    @DecimalMin(value = "0.01", message = "Wholesale price must be positive")
    private BigDecimal wholesalePrice;

    @Min(value = 1, message = "Wholesale MOQ must be at least 1")
    private int wholesaleMoq = 10;

    private String imageUrl;
    private boolean featured = false;
    private boolean active = true;

    public CreateProductRequest() {}

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProductGrade getGrade() { return grade; }
    public void setGrade(ProductGrade grade) { this.grade = grade; }

    public PackageType getPackageType() { return packageType; }
    public void setPackageType(PackageType packageType) { this.packageType = packageType; }

    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public void setUnitWeightKg(BigDecimal unitWeightKg) { this.unitWeightKg = unitWeightKg; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public int getWholesaleMoq() { return wholesaleMoq; }
    public void setWholesaleMoq(int wholesaleMoq) { this.wholesaleMoq = wholesaleMoq; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
