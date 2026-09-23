package com.rrjaggery.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BatchWastageRequest {

    @NotBlank(message = "Material SKU is required")
    private String materialSku;

    private String materialName;

    @NotNull(message = "Wastage quantity is required")
    @DecimalMin(value = "0.001", inclusive = false, message = "Wastage quantity must be greater than 0")
    private BigDecimal wastageQuantity;

    private String unitOfMeasure = "KG";
    private String reason;
    private String wastageType = "SCRAP"; // SCRAP, LOSS, SPILLAGE, DEFECT
    private String actor;
    private String idempotencyKey;

    public BatchWastageRequest() {
    }

    public BatchWastageRequest(String materialSku, String materialName, BigDecimal wastageQuantity, String unitOfMeasure, String reason, String wastageType, String actor) {
        this.materialSku = materialSku;
        this.materialName = materialName;
        this.wastageQuantity = wastageQuantity;
        this.unitOfMeasure = unitOfMeasure;
        this.reason = reason;
        this.wastageType = wastageType;
        this.actor = actor;
    }

    public String getMaterialSku() {
        return materialSku;
    }

    public void setMaterialSku(String materialSku) {
        this.materialSku = materialSku;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public BigDecimal getWastageQuantity() {
        return wastageQuantity;
    }

    public void setWastageQuantity(BigDecimal wastageQuantity) {
        this.wastageQuantity = wastageQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getWastageType() {
        return wastageType;
    }

    public void setWastageType(String wastageType) {
        this.wastageType = wastageType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
