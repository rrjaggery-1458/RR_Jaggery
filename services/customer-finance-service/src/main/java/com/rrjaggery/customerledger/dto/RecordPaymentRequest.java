package com.rrjaggery.customerledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class RecordPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // CASH, BANK_TRANSFER, UPI, CHEQUE

    private UUID offlineOrderId; // Optional: specify if payment is against a specific offline order/invoice

    private String referenceNotes;

    private String idempotencyKey;

    public RecordPaymentRequest() {}

    public RecordPaymentRequest(BigDecimal amount, String paymentMethod, UUID offlineOrderId,
                                String referenceNotes, String idempotencyKey) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.offlineOrderId = offlineOrderId;
        this.referenceNotes = referenceNotes;
        this.idempotencyKey = idempotencyKey;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public UUID getOfflineOrderId() { return offlineOrderId; }
    public void setOfflineOrderId(UUID offlineOrderId) { this.offlineOrderId = offlineOrderId; }

    public String getReferenceNotes() { return referenceNotes; }
    public void setReferenceNotes(String referenceNotes) { this.referenceNotes = referenceNotes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
