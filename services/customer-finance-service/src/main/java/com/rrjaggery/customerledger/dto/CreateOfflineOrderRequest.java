package com.rrjaggery.customerledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateOfflineOrderRequest {

    private UUID customerId;

    private String customerName;

    private String orderType = "WHOLESALE"; // WHOLESALE, MANUAL_RETAIL

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OfflineOrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private String paymentMethod = "CREDIT"; // CREDIT, CASH, PARTIAL_CASH, UPI, BANK_TRANSFER

    @DecimalMin(value = "0.00", message = "Immediate payment amount cannot be negative")
    private BigDecimal immediatePaidAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Delivery charges cannot be negative")
    private BigDecimal deliveryCharges = BigDecimal.ZERO;

    private String orderStatus;

    private String paymentStatus;

    private Instant orderDate;

    private String notes;

    private String idempotencyKey;

    public CreateOfflineOrderRequest() {}

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public List<OfflineOrderItemRequest> getItems() { return items; }
    public void setItems(List<OfflineOrderItemRequest> items) { this.items = items; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getImmediatePaidAmount() { return immediatePaidAmount; }
    public void setImmediatePaidAmount(BigDecimal immediatePaidAmount) { this.immediatePaidAmount = immediatePaidAmount; }

    public BigDecimal getDeliveryCharges() { return deliveryCharges; }
    public void setDeliveryCharges(BigDecimal deliveryCharges) { this.deliveryCharges = deliveryCharges; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Instant getOrderDate() { return orderDate; }
    public void setOrderDate(Instant orderDate) { this.orderDate = orderDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
