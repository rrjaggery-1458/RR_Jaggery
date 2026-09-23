package com.rrjaggery.commerce.dto;

import jakarta.validation.constraints.Min;

public class UpdateCartItemRequest {

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;

    public UpdateCartItemRequest() {}

    public UpdateCartItemRequest(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
