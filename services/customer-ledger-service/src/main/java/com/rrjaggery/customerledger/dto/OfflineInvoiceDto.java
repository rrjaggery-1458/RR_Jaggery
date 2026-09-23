package com.rrjaggery.customerledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OfflineInvoiceDto {

    private String invoiceNumber;
    private UUID orderId;
    private String orderNumber;
    private Instant invoiceDate;
    private Instant dueDate;
    private boolean overdue;

    // Seller Info (Mandya Mill)
    private String sellerName = "RR JAGGERY TRADERS";
    private String sellerGstin = "29AABCR1234F1Z5";
    private String sellerAddress = "Sy. No. 45/2, Mandya Sugar Complex Road, Mandya, Karnataka - 571401";
    private String sellerContact = "+91 9845012345 | sales@rrjaggery.com";

    // Buyer Info
    private UUID customerId;
    private String customerName;
    private String businessName;
    private String customerGstin;
    private String customerPhone;
    private String customerEmail;
    private AddressDto billingAddress;
    private AddressDto shippingAddress;

    // Line items & tax breakdown
    private List<OfflineOrderItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalTax;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String paymentMethod;
    private String paymentStatus;

    public OfflineInvoiceDto() {}

    // Getters and Setters
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Instant getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(Instant invoiceDate) { this.invoiceDate = invoiceDate; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerGstin() { return sellerGstin; }
    public void setSellerGstin(String sellerGstin) { this.sellerGstin = sellerGstin; }

    public String getSellerAddress() { return sellerAddress; }
    public void setSellerAddress(String sellerAddress) { this.sellerAddress = sellerAddress; }

    public String getSellerContact() { return sellerContact; }
    public void setSellerContact(String sellerContact) { this.sellerContact = sellerContact; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getCustomerGstin() { return customerGstin; }
    public void setCustomerGstin(String customerGstin) { this.customerGstin = customerGstin; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public AddressDto getBillingAddress() { return billingAddress; }
    public void setBillingAddress(AddressDto billingAddress) { this.billingAddress = billingAddress; }

    public AddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public List<OfflineOrderItemDto> getItems() { return items; }
    public void setItems(List<OfflineOrderItemDto> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }

    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }

    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
