package com.rrjaggery.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BatchCostingDto {

    private UUID batchId;
    private String batchNumber;
    private String batchStatus;
    private BigDecimal finishedQuantity = BigDecimal.ZERO;
    private String unitOfMeasure = "KG";

    // Cost components
    private BigDecimal rawMaterialCost = BigDecimal.ZERO;
    private BigDecimal labourCost = BigDecimal.ZERO;
    private BigDecimal fuelCost = BigDecimal.ZERO;
    private BigDecimal packagingCost = BigDecimal.ZERO;
    private BigDecimal otherCost = BigDecimal.ZERO;
    private BigDecimal totalBatchCost = BigDecimal.ZERO;

    // Per-unit cost (null if batch is cancelled or zero output)
    private BigDecimal costPerKg = null;

    private String costingStatus; // VALID, COSTING_INVALID (zero output / cancelled)
    private String costingNotes;

    public BatchCostingDto() {}

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }

    public BigDecimal getFinishedQuantity() { return finishedQuantity; }
    public void setFinishedQuantity(BigDecimal finishedQuantity) { this.finishedQuantity = finishedQuantity; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public BigDecimal getRawMaterialCost() { return rawMaterialCost; }
    public void setRawMaterialCost(BigDecimal rawMaterialCost) { this.rawMaterialCost = rawMaterialCost; }

    public BigDecimal getLabourCost() { return labourCost; }
    public void setLabourCost(BigDecimal labourCost) { this.labourCost = labourCost; }

    public BigDecimal getFuelCost() { return fuelCost; }
    public void setFuelCost(BigDecimal fuelCost) { this.fuelCost = fuelCost; }

    public BigDecimal getPackagingCost() { return packagingCost; }
    public void setPackagingCost(BigDecimal packagingCost) { this.packagingCost = packagingCost; }

    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }

    public BigDecimal getTotalBatchCost() { return totalBatchCost; }
    public void setTotalBatchCost(BigDecimal totalBatchCost) { this.totalBatchCost = totalBatchCost; }

    public BigDecimal getCostPerKg() { return costPerKg; }
    public void setCostPerKg(BigDecimal costPerKg) { this.costPerKg = costPerKg; }

    public String getCostingStatus() { return costingStatus; }
    public void setCostingStatus(String costingStatus) { this.costingStatus = costingStatus; }

    public String getCostingNotes() { return costingNotes; }
    public void setCostingNotes(String costingNotes) { this.costingNotes = costingNotes; }
}
