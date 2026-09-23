package com.rrjaggery.procurement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines", schema = "procurement_schema",
        indexes = {
                @Index(name = "idx_purchase_order_line_po", columnList = "purchase_order_id"),
                @Index(name = "idx_purchase_order_line_sku", columnList = "sku")
        })
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "item_name", nullable = false, length = 160)
    private String itemName;

    @Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal orderedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    public PurchaseOrderLine() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
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

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity == null ? BigDecimal.ZERO : receivedQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
