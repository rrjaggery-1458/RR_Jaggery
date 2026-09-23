package com.rrjaggery.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BatchConsumptionRequest {

    @NotBlank(message = "Raw material SKU is required")
    private String rawMaterialSku;

    private String rawMaterialName;

    @NotNull(message = "Consumed quantity is required")
    @DecimalMin(value = "0.001", inclusive = false, message = "Consumed quantity must be greater than 0")
    private BigDecimal consumedQuantity;

    private String unitOfMeasure = "KG";
    private String actor;
    private String notes;
    private String idempotencyKey;

    public BatchConsumptionRequest() {
    }

    public BatchConsumptionRequest(String rawMaterialSku, String rawMaterialName, BigDecimal consumedQuantity, String unitOfMeasure, String actor) {
        this.rawMaterialSku = rawMaterialSku;
        this.rawMaterialName = rawMaterialName;
        this.consumedQuantity = consumedQuantity;
        this.unitOfMeasure = unitOfMeasure;
        this.actor = actor;
    }

    public String getRawMaterialSku() {
        return rawMaterialSku;
    }

    public void setRawMaterialSku(String rawMaterialSku) {
        this.rawMaterialSku = rawMaterialSku;
    }

    public String getRawMaterialName() {
        return rawMaterialName;
    }

    public void setRawMaterialName(String rawMaterialName) {
        this.rawMaterialName = rawMaterialName;
    }

    public BigDecimal getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(BigDecimal consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
