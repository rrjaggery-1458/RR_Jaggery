package com.rrjaggery.customerledger.dto;

import java.math.BigDecimal;

public class UpdateOfflineOrderRequest {
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal deliveryCharges;
    private String notes;

    public UpdateOfflineOrderRequest() {}

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getDeliveryCharges() { return deliveryCharges; }
    public void setDeliveryCharges(BigDecimal deliveryCharges) { this.deliveryCharges = deliveryCharges; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
