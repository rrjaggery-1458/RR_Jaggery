package com.rrjaggery.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "commerce_schema")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductGrade grade = ProductGrade.GRADE_A_TRADITIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 30)
    private PackageType packageType = PackageType.BOX;

    @Column(name = "unit_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal unitWeightKg = BigDecimal.ONE;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "wholesale_moq", nullable = false)
    private int wholesaleMoq = 10;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Product() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
