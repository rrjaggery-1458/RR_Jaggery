package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.OfflineOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OfflineOrderDto {

    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String businessName;
    private String customerPhone;
    private String customerEmail;
    private String gstin;
    private String invoiceNumber;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private Instant orderDate;
    private Instant dueDate;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private boolean overdue;
    private String notes;
    private List<OfflineOrderItemDto> items;
    private Instant createdAt;

    public OfflineOrderDto() {}

    public static OfflineOrderDto fromEntity(OfflineOrder o) {
        OfflineOrderDto dto = new OfflineOrderDto();
        dto.setId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());
        dto.setCustomerId(o.getCustomer().getId());
        dto.setCustomerName(o.getCustomer().getContactPerson());
        dto.setBusinessName(o.getCustomer().getBusinessName());
        dto.setCustomerPhone(o.getCustomer().getPhone());
        dto.setCustomerEmail(o.getCustomer().getEmail());
        dto.setGstin(o.getCustomer().getGstin());
        dto.setInvoiceNumber(o.getInvoiceNumber());
        dto.setSubtotalAmount(o.getSubtotalAmount());
        dto.setTaxAmount(o.getTaxAmount());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setPaidAmount(o.getPaidAmount());
        dto.setOutstandingAmount(o.getOutstandingAmount());
        dto.setOrderDate(o.getOrderDate());
        dto.setDueDate(o.getDueDate());
        dto.setStatus(o.getStatus());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setOverdue(o.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0 && o.getDueDate().isBefore(Instant.now()));
        dto.setNotes(o.getNotes());
        dto.setItems(o.getItems().stream().map(OfflineOrderItemDto::fromEntity).collect(Collectors.toList()));
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    public Instant getOrderDate() { return orderDate; }
    public void setOrderDate(Instant orderDate) { this.orderDate = orderDate; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<OfflineOrderItemDto> getItems() { return items; }
    public void setItems(List<OfflineOrderItemDto> items) { this.items = items; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
