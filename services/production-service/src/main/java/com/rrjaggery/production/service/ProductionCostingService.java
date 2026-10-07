package com.rrjaggery.production.service;

import com.rrjaggery.production.dto.BatchCostingDto;
import com.rrjaggery.production.entity.BatchConsumption;
import com.rrjaggery.production.entity.BatchOutput;
import com.rrjaggery.production.entity.BatchStatus;
import com.rrjaggery.production.entity.ProductionBatch;
import com.rrjaggery.production.entity.ProductionCostMaster;
import com.rrjaggery.production.repository.ProductionBatchRepository;
import com.rrjaggery.production.repository.ProductionCostMasterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ProductionCostingService {

    private final ProductionBatchRepository batchRepository;
    private final ProductionCostMasterRepository costMasterRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductionCostingService(
            ProductionBatchRepository batchRepository,
            ProductionCostMasterRepository costMasterRepository,
            JdbcTemplate jdbcTemplate) {
        this.batchRepository = batchRepository;
        this.costMasterRepository = costMasterRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public BatchCostingDto computeBatchCosting(UUID batchId) {
        ProductionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Production batch not found: " + batchId));

        BatchCostingDto dto = new BatchCostingDto();
        dto.setBatchId(batch.getId());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setBatchStatus(batch.getStatus().name());
        dto.setUnitOfMeasure(batch.getUnitOfMeasure());

        // Guard: cancelled batch cannot have valid costing
        if (batch.getStatus() == BatchStatus.CANCELLED) {
            dto.setFinishedQuantity(BigDecimal.ZERO);
            dto.setCostingStatus("COSTING_INVALID");
            dto.setCostingNotes("Batch is cancelled. Costing is not applicable.");
            return dto;
        }

        // Determine finished output quantity from batch_outputs
        BigDecimal finishedQty = BigDecimal.ZERO;
        if (batch.getOutputs() != null) {
            for (BatchOutput output : batch.getOutputs()) {
                if (output.getQuantityProduced() != null) {
                    finishedQty = finishedQty.add(output.getQuantityProduced());
                }
            }
        }
        if (finishedQty.compareTo(BigDecimal.ZERO) == 0) {
            finishedQty = batch.getActualQuantity() != null ? batch.getActualQuantity() : BigDecimal.ZERO;
        }
        dto.setFinishedQuantity(finishedQty);

        // ── 1. Dynamic Raw Material Valuation from Inventory / PO Price ─────
        BigDecimal rawMaterialCost = BigDecimal.ZERO;
        if (batch.getConsumptions() != null) {
            for (BatchConsumption consumption : batch.getConsumptions()) {
                BigDecimal qty = consumption.getConsumedQuantity() != null ? consumption.getConsumedQuantity() : BigDecimal.ZERO;
                BigDecimal unitCost = fetchDynamicRawMaterialUnitPrice(consumption.getRawMaterialSku(), consumption.getRawMaterialName());
                rawMaterialCost = rawMaterialCost.add(qty.multiply(unitCost));
            }
        }
        rawMaterialCost = rawMaterialCost.setScale(2, RoundingMode.HALF_UP);
        dto.setRawMaterialCost(rawMaterialCost);

        // Guard: if zero finished quantity, costing is invalid (division by zero)
        if (finishedQty.compareTo(BigDecimal.ZERO) <= 0) {
            dto.setTotalBatchCost(rawMaterialCost);
            dto.setCostPerKg(null);
            dto.setCostingStatus("COSTING_INVALID");
            dto.setCostingNotes("Finished output quantity is zero. Cost per KG cannot be computed.");
            return dto;
        }

        // ── 2. Configurable Production Cost Allocation (Effective-Dated) ───
        LocalDate batchDate = LocalDate.now();
        if (batch.getCompletionDate() != null) {
            batchDate = batch.getCompletionDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } else if (batch.getActualStartDate() != null) {
            batchDate = batch.getActualStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }

        List<ProductionCostMaster> activeCosts = costMasterRepository.findActiveCostsForDate(batchDate);

        BigDecimal labourCost = BigDecimal.ZERO;
        BigDecimal fuelCost = BigDecimal.ZERO;
        BigDecimal packagingCost = BigDecimal.ZERO;
        BigDecimal otherCost = BigDecimal.ZERO;

        for (ProductionCostMaster master : activeCosts) {
            BigDecimal computedAmount = BigDecimal.ZERO;
            String basis = master.getCalculationBasis() != null ? master.getCalculationBasis().toUpperCase() : "PER_KG_OUTPUT";

            switch (basis) {
                case "PER_KG_OUTPUT":
                case "PER_UNIT_OUTPUT":
                case "PER_LITRE":
                    computedAmount = finishedQty.multiply(master.getRate());
                    break;
                case "PER_BATCH":
                case "FIXED_PER_BATCH":
                    computedAmount = master.getRate();
                    break;
                case "MANUAL_ALLOCATION":
                case "ACTUAL_EXPENSE":
                    computedAmount = master.getRate();
                    break;
                default:
                    computedAmount = finishedQty.multiply(master.getRate());
                    break;
            }

            String type = master.getCostType() != null ? master.getCostType().toUpperCase() : "OTHER";
            if (type.contains("LABOUR") || type.contains("LABOR") || master.getCostName().toUpperCase().contains("LABOUR")) {
                labourCost = labourCost.add(computedAmount);
            } else if (type.contains("FUEL") || master.getCostName().toUpperCase().contains("FUEL") || master.getCostName().toUpperCase().contains("DIESEL") || master.getCostName().toUpperCase().contains("WOOD")) {
                fuelCost = fuelCost.add(computedAmount);
            } else if (type.contains("PACKAGING") || master.getCostName().toUpperCase().contains("PACKAGING") || master.getCostName().toUpperCase().contains("BAG")) {
                packagingCost = packagingCost.add(computedAmount);
            } else {
                otherCost = otherCost.add(computedAmount);
            }
        }

        labourCost = labourCost.setScale(2, RoundingMode.HALF_UP);
        fuelCost = fuelCost.setScale(2, RoundingMode.HALF_UP);
        packagingCost = packagingCost.setScale(2, RoundingMode.HALF_UP);
        otherCost = otherCost.setScale(2, RoundingMode.HALF_UP);

        dto.setLabourCost(labourCost);
        dto.setFuelCost(fuelCost);
        dto.setPackagingCost(packagingCost);
        dto.setOtherCost(otherCost);

        // ── 3. Total Batch Cost ───────────────────────────────────────────
        BigDecimal totalBatchCost = rawMaterialCost
                .add(labourCost)
                .add(fuelCost)
                .add(packagingCost)
                .add(otherCost)
                .setScale(2, RoundingMode.HALF_UP);
        dto.setTotalBatchCost(totalBatchCost);

        // ── 4. Cost Per KG Output ─────────────────────────────────────────
        BigDecimal costPerKg = totalBatchCost.divide(finishedQty, 2, RoundingMode.HALF_UP);
        dto.setCostPerKg(costPerKg);

        dto.setCostingStatus("VALID");
        dto.setCostingNotes(batch.getStatus() == BatchStatus.COMPLETED
                ? "Costing computed dynamically from configured Production Cost Master rules for date " + batchDate + "."
                : "Preliminary costing – batch not yet completed.");

        return dto;
    }

    /**
     * Dynamically fetch unit price using the Actual Purchase / Receipt Cost Valuation Method.
     * Looks up latest PO line unit_cost from procurement_schema.purchase_order_lines,
     * with fallback to configured RAW_MATERIAL rate in production_cost_masters.
     */
    private BigDecimal fetchDynamicRawMaterialUnitPrice(String sku, String name) {
        try {
            if (sku != null && !sku.isBlank()) {
                List<BigDecimal> poResults = jdbcTemplate.query(
                        "SELECT unit_cost FROM procurement_schema.purchase_order_lines WHERE LOWER(sku) = LOWER(?) ORDER BY unit_cost DESC LIMIT 1",
                        (rs, rowNum) -> rs.getBigDecimal(1),
                        sku.trim()
                );
                if (!poResults.isEmpty() && poResults.get(0) != null && poResults.get(0).compareTo(BigDecimal.ZERO) > 0) {
                    return poResults.get(0);
                }
            }

            if (name != null && !name.isBlank()) {
                List<BigDecimal> poResults = jdbcTemplate.query(
                        "SELECT unit_cost FROM procurement_schema.purchase_order_lines WHERE LOWER(item_name) LIKE LOWER(?) ORDER BY unit_cost DESC LIMIT 1",
                        (rs, rowNum) -> rs.getBigDecimal(1),
                        "%" + name.trim() + "%"
                );
                if (!poResults.isEmpty() && poResults.get(0) != null && poResults.get(0).compareTo(BigDecimal.ZERO) > 0) {
                    return poResults.get(0);
                }
            }

            // Check if there is a configured RAW_MATERIAL rate in production_cost_masters
            List<BigDecimal> masterRates = jdbcTemplate.query(
                    "SELECT rate FROM production_schema.production_cost_masters WHERE active = true AND (cost_type = 'RAW_MATERIAL' OR cost_code LIKE 'RM%') LIMIT 1",
                    (rs, rowNum) -> rs.getBigDecimal(1)
            );
            if (!masterRates.isEmpty() && masterRates.get(0) != null && masterRates.get(0).compareTo(BigDecimal.ZERO) > 0) {
                return masterRates.get(0);
            }
        } catch (Exception e) {
            // Fallback handled
        }
        return BigDecimal.ZERO;
    }
}
