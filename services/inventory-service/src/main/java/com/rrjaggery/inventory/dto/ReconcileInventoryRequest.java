package com.rrjaggery.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ReconcileInventoryRequest {
    @NotNull(message = "Inventory item ID is required")
    private UUID itemId;

    @NotNull(message = "Counted quantity is required")
    private BigDecimal countedQuantity;

    @NotNull(message = "Reason is required")
    private String reason;

    private String actor = "ADMIN";
    private String notes;

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(BigDecimal countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
