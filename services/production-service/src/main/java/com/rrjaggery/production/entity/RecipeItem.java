package com.rrjaggery.production.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "recipe_items", schema = "production_schema",
        indexes = {
                @Index(name = "idx_recipe_item_recipe", columnList = "recipe_id"),
                @Index(name = "idx_recipe_item_sku", columnList = "raw_material_sku")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "raw_material_sku", nullable = false, length = 100)
    private String rawMaterialSku;

    @Column(name = "raw_material_name", nullable = false, length = 180)
    private String rawMaterialName;

    @Column(name = "required_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal requiredQuantity;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "KG";

    @Column(name = "is_optional", nullable = false)
    private boolean isOptional = false;

    @Column(name = "notes", length = 500)
    private String notes;

    public RecipeItem() {
    }

    public RecipeItem(String rawMaterialSku, String rawMaterialName, BigDecimal requiredQuantity, String unitOfMeasure, boolean isOptional) {
        this.rawMaterialSku = rawMaterialSku;
        this.rawMaterialName = rawMaterialName;
        this.requiredQuantity = requiredQuantity;
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
        this.isOptional = isOptional;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getRawMaterialSku() {
        return rawMaterialSku;
    }

    public void setRawMaterialSku(String rawMaterialSku) {
        this.rawMaterialSku = rawMaterialSku;
    }

    public String getRawMaterialName() {
        return rawMaterialName;
    }

    public void setRawMaterialName(String rawMaterialName) {
        this.rawMaterialName = rawMaterialName;
    }

    public BigDecimal getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(BigDecimal requiredQuantity) {
        this.requiredQuantity = requiredQuantity == null ? BigDecimal.ZERO : requiredQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
