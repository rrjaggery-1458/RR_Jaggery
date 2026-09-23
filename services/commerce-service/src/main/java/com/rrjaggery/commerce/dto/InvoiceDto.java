package com.rrjaggery.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class InvoiceDto {

    private String invoiceNumber;
    private UUID orderId;
    private String orderNumber;
    private Instant invoiceDate;
    private String sellerName = "RR JAGGERY TRADERS";
    private String sellerGstin = "29AABCR1234F1Z5";
    private String sellerAddress = "Mandya Jaggery Processing Unit 4, Highway 275, Mandya, Karnataka - 571401";
    private String sellerContact = "+91 8232 220011 | accounts@rrjaggery.com";
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private AddressDto shippingAddress;
    private List<OrderItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalTax;
    private BigDecimal shippingAmount;
    private BigDecimal grandTotal;
    private String paymentMethod;
    private String paymentStatus;

    public InvoiceDto() {}

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Instant getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(Instant invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSellerGstin() { return sellerGstin; }
    public void setSellerGstin(String sellerGstin) { this.sellerGstin = sellerGstin; }

    public String getSellerAddress() { return sellerAddress; }
    public void setSellerAddress(String sellerAddress) { this.sellerAddress = sellerAddress; }

    public String getSellerContact() { return sellerContact; }
    public void setSellerContact(String sellerContact) { this.sellerContact = sellerContact; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public AddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }

    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }

    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
