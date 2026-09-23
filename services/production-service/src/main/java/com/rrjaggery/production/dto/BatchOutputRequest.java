package com.rrjaggery.production.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BatchOutputRequest {

    private String outputProductSku;
    private String outputProductName;

    @NotNull(message = "Quantity produced is required")
    @DecimalMin(value = "0.001", inclusive = false, message = "Quantity produced must be greater than 0")
    private BigDecimal quantityProduced;

    private String unitOfMeasure = "KG";
    private String batchLot;
    private String qualityGrade = "A_GRADE"; // A_GRADE, B_GRADE, PREMIUM
    private String actor;
    private String notes;
    private String idempotencyKey;

    public BatchOutputRequest() {
    }

    public BatchOutputRequest(String outputProductSku, String outputProductName, BigDecimal quantityProduced, String unitOfMeasure, String batchLot, String qualityGrade, String actor) {
        this.outputProductSku = outputProductSku;
        this.outputProductName = outputProductName;
        this.quantityProduced = quantityProduced;
        this.unitOfMeasure = unitOfMeasure;
        this.batchLot = batchLot;
        this.qualityGrade = qualityGrade;
        this.actor = actor;
    }

    public String getOutputProductSku() {
        return outputProductSku;
    }

    public void setOutputProductSku(String outputProductSku) {
        this.outputProductSku = outputProductSku;
    }

    public String getOutputProductName() {
        return outputProductName;
    }

    public void setOutputProductName(String outputProductName) {
        this.outputProductName = outputProductName;
    }

    public BigDecimal getQuantityProduced() {
        return quantityProduced;
    }

    public void setQuantityProduced(BigDecimal quantityProduced) {
        this.quantityProduced = quantityProduced;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getBatchLot() {
        return batchLot;
    }

    public void setBatchLot(String batchLot) {
        this.batchLot = batchLot;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
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
