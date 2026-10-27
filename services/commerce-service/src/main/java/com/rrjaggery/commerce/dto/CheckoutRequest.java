package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressDto shippingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String notes;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    public CheckoutRequest() {}

    public CheckoutRequest(AddressDto shippingAddress, PaymentMethod paymentMethod, String notes) {
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public CheckoutRequest(AddressDto shippingAddress, PaymentMethod paymentMethod, String notes,
                           String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpaySignature = razorpaySignature;
    }

    public AddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public void setRazorpay_order_id(String id) { this.razorpayOrderId = id; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public void setRazorpay_payment_id(String id) { this.razorpayPaymentId = id; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    public void setRazorpay_signature(String sig) { this.razorpaySignature = sig; }
}
