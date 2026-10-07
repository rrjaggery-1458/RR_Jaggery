package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.OrderStatus;
import com.rrjaggery.commerce.entity.PaymentStatus;

public class UpdateOrderStatusRequest {

    private OrderStatus status;
    private PaymentStatus paymentStatus;

    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public UpdateOrderStatusRequest(OrderStatus status, PaymentStatus paymentStatus) {
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
}
