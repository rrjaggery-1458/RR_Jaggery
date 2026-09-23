package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.CustomerPayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CustomerPaymentDto {

    private UUID id;
    private String paymentNumber;
    private UUID customerId;
    private String customerName;
    private String businessName;
    private UUID offlineOrderId;
    private String orderNumber;
    private String invoiceNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private Instant paymentDate;
    private String referenceNotes;
    private String idempotencyKey;
    private Instant createdAt;

    public CustomerPaymentDto() {}

    public static CustomerPaymentDto fromEntity(CustomerPayment p) {
        CustomerPaymentDto dto = new CustomerPaymentDto();
        dto.setId(p.getId());
        dto.setPaymentNumber(p.getPaymentNumber());
        dto.setCustomerId(p.getCustomer().getId());
        dto.setCustomerName(p.getCustomer().getContactPerson());
        dto.setBusinessName(p.getCustomer().getBusinessName());
        if (p.getOfflineOrder() != null) {
            dto.setOfflineOrderId(p.getOfflineOrder().getId());
            dto.setOrderNumber(p.getOfflineOrder().getOrderNumber());
            dto.setInvoiceNumber(p.getOfflineOrder().getInvoiceNumber());
        }
        dto.setAmount(p.getAmount());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setReferenceNotes(p.getReferenceNotes());
        dto.setIdempotencyKey(p.getIdempotencyKey());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public UUID getOfflineOrderId() { return offlineOrderId; }
    public void setOfflineOrderId(UUID offlineOrderId) { this.offlineOrderId = offlineOrderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Instant getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceNotes() { return referenceNotes; }
    public void setReferenceNotes(String referenceNotes) { this.referenceNotes = referenceNotes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
