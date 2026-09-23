package com.rrjaggery.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    public AddToCartRequest() {}

    public AddToCartRequest(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
