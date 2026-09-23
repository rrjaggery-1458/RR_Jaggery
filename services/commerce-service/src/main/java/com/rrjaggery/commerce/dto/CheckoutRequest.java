package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDto shippingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod = PaymentMethod.TEST_PAYMENT;

    private String notes;

    public CheckoutRequest() {}

    public CheckoutRequest(AddressDto shippingAddress, PaymentMethod paymentMethod, String notes) {
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public AddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
