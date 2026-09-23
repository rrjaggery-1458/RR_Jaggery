package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recipes", schema = "production_schema",
        uniqueConstraints = @UniqueConstraint(name = "uk_recipe_code", columnNames = "recipe_code"),
        indexes = {
                @Index(name = "idx_recipe_target_sku", columnList = "output_product_sku"),
                @Index(name = "idx_recipe_active", columnList = "active")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipe_code", nullable = false, length = 60)
    private String recipeCode;

    @Column(name = "recipe_name", nullable = false, length = 180)
    private String recipeName;

    @Column(name = "output_product_sku", nullable = false, length = 100)
    private String outputProductSku;

    @Column(name = "output_product_name", nullable = false, length = 180)
    private String outputProductName;

    @Column(name = "standard_batch_size", nullable = false, precision = 19, scale = 3)
    private BigDecimal standardBatchSize = BigDecimal.valueOf(100);

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Column(name = "standard_yield_percentage", precision = 7, scale = 2)
    private BigDecimal standardYieldPercentage = new BigDecimal("100.00");

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes = 60;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeItem> items = new ArrayList<>();

    public Recipe() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getName() {
        return recipeName;
    }

    public void setName(String name) {
        this.recipeName = name;
    }

    public String getOutputProductSku() {
        return outputProductSku;
    }

    public void setOutputProductSku(String outputProductSku) {
        this.outputProductSku = outputProductSku;
    }

    public String getTargetProductSku() {
        return outputProductSku;
    }

    public void setTargetProductSku(String targetProductSku) {
        this.outputProductSku = targetProductSku;
    }

    public String getOutputProductName() {
        return outputProductName;
    }

    public void setOutputProductName(String outputProductName) {
        this.outputProductName = outputProductName;
    }

    public String getTargetProductName() {
        return outputProductName;
    }

    public void setTargetProductName(String targetProductName) {
        this.outputProductName = targetProductName;
    }

    public BigDecimal getStandardBatchSize() {
        return standardBatchSize;
    }

    public void setStandardBatchSize(BigDecimal standardBatchSize) {
        this.standardBatchSize = standardBatchSize == null ? BigDecimal.ZERO : standardBatchSize;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public BigDecimal getStandardYieldPercentage() {
        return standardYieldPercentage;
    }

    public void setStandardYieldPercentage(BigDecimal standardYieldPercentage) {
        this.standardYieldPercentage = standardYieldPercentage;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getIsActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setIsActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RecipeItem> getItems() {
        return items;
    }

    public void setItems(List<RecipeItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public void addItem(RecipeItem item) {
        item.setRecipe(this);
        this.items.add(item);
    }
}
