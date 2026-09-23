package com.rrjaggery.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "commerce_schema")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    public OrderItem() {}

    public OrderItem(UUID productId, String productName, String sku, BigDecimal unitPrice, int quantity, BigDecimal unitWeightKg, BigDecimal lineTotal, BigDecimal taxAmount) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.unitWeightKg = unitWeightKg;
        this.lineTotal = lineTotal;
        this.taxAmount = taxAmount;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public void setUnitWeightKg(BigDecimal unitWeightKg) { this.unitWeightKg = unitWeightKg; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
}
