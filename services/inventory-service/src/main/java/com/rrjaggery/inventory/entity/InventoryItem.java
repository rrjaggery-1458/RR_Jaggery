package com.rrjaggery.inventory.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items", schema = "inventory_schema",
        indexes = {
                @Index(name = "idx_inventory_item_sku", columnList = "sku", unique = true),
                @Index(name = "idx_inventory_item_status", columnList = "status")
        })
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType = ItemType.RAW_MATERIAL;

    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure = "KG";

    @Column(name = "current_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "minimum_stock_level", nullable = false, precision = 14, scale = 3)
    private BigDecimal minimumStockLevel = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "batch_lot", length = 100)
    private String batchLot;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public InventoryItem() {
    }

    public InventoryItem(String sku, String itemName, ItemType itemType, String unitOfMeasure,
                        BigDecimal minimumStockLevel, String batchLot, String sourceReference) {
        this.sku = sku;
        this.itemName = itemName;
        this.itemType = itemType == null ? ItemType.RAW_MATERIAL : itemType;
        this.unitOfMeasure = unitOfMeasure == null || unitOfMeasure.isBlank() ? "KG" : unitOfMeasure;
        this.minimumStockLevel = minimumStockLevel == null ? BigDecimal.ZERO : minimumStockLevel;
        this.batchLot = batchLot;
        this.sourceReference = sourceReference;
        this.currentQuantity = BigDecimal.ZERO;
        this.reservedQuantity = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
    }

    public BigDecimal getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public void setMinimumStockLevel(BigDecimal minimumStockLevel) {
        this.minimumStockLevel = minimumStockLevel == null ? BigDecimal.ZERO : minimumStockLevel;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        this.reservedQuantity = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBatchLot() {
        return batchLot;
    }

    public void setBatchLot(String batchLot) {
        this.batchLot = batchLot;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
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
}
