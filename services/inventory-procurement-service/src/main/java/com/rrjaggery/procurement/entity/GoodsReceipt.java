package com.rrjaggery.procurement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts", schema = "procurement_schema",
        indexes = {
                @Index(name = "idx_goods_receipt_po", columnList = "purchase_order_id"),
                @Index(name = "idx_goods_receipt_sku", columnList = "sku"),
                @Index(name = "idx_goods_receipt_idempotency", columnList = "idempotency_key", unique = true)
        })
public class GoodsReceipt {

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

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "receipt_timestamp", nullable = false)
    private Instant receiptTimestamp = Instant.now();

    @Column(name = "received_by", nullable = false, length = 120)
    private String receivedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    public GoodsReceipt() {
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

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Instant getReceiptTimestamp() {
        return receiptTimestamp;
    }

    public void setReceiptTimestamp(Instant receiptTimestamp) {
        this.receiptTimestamp = receiptTimestamp;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
