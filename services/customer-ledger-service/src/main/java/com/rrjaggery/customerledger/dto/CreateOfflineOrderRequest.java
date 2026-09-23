package com.rrjaggery.customerledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOfflineOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OfflineOrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private String paymentMethod = "CREDIT"; // CREDIT, CASH, PARTIAL_CASH

    @DecimalMin(value = "0.00", message = "Immediate payment amount cannot be negative")
    private BigDecimal immediatePaidAmount = BigDecimal.ZERO;

    private String notes;

    private String idempotencyKey;

    public CreateOfflineOrderRequest() {}

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public List<OfflineOrderItemRequest> getItems() { return items; }
    public void setItems(List<OfflineOrderItemRequest> items) { this.items = items; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getImmediatePaidAmount() { return immediatePaidAmount; }
    public void setImmediatePaidAmount(BigDecimal immediatePaidAmount) { this.immediatePaidAmount = immediatePaidAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
