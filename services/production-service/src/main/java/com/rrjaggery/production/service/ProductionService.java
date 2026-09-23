package com.rrjaggery.production.service;

import com.rrjaggery.production.dto.*;
import com.rrjaggery.production.entity.*;
import com.rrjaggery.production.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductionService {

    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ProductionBatchRepository batchRepository;
    private final RecipeRepository recipeRepository;
    private final BatchConsumptionRepository consumptionRepository;
    private final BatchOutputRepository outputRepository;
    private final BatchWastageRepository wastageRepository;
    private final InventoryStockClient inventoryStockClient;

    public ProductionService(ProductionBatchRepository batchRepository,
                             RecipeRepository recipeRepository,
                             BatchConsumptionRepository consumptionRepository,
                             BatchOutputRepository outputRepository,
                             BatchWastageRepository wastageRepository,
                             InventoryStockClient inventoryStockClient) {
        this.batchRepository = batchRepository;
        this.recipeRepository = recipeRepository;
        this.consumptionRepository = consumptionRepository;
        this.outputRepository = outputRepository;
        this.wastageRepository = wastageRepository;
        this.inventoryStockClient = inventoryStockClient;
    }

    @Transactional
    public ProductionBatchDto createBatch(CreateBatchRequest request) {
        Recipe recipe = null;
        if (request.getRecipeId() != null) {
            recipe = recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> new IllegalArgumentException("Recipe not found for ID: " + request.getRecipeId()));
        } else if (request.getRecipeCode() != null && !request.getRecipeCode().isBlank()) {
            recipe = recipeRepository.findByRecipeCodeIgnoreCase(request.getRecipeCode().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Recipe not found for code: " + request.getRecipeCode()));
        }

        String targetSku = request.getTargetProductSku() != null ? request.getTargetProductSku().trim().toUpperCase(Locale.ROOT) : null;
        String targetName = request.getTargetProductName();

        if (recipe != null) {
            if (targetSku == null || targetSku.isBlank()) {
                targetSku = recipe.getOutputProductSku();
            }
            if (targetName == null || targetName.isBlank()) {
                targetName = recipe.getOutputProductName();
            }
        }

        if (targetSku == null || targetSku.isBlank()) {
            throw new IllegalArgumentException("Target product SKU is required.");
        }
        if (targetName == null || targetName.isBlank()) {
            targetName = targetSku;
        }

        String dateTag = DATE_PREFIX.format(Instant.now());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String batchNumber = "BATCH-" + dateTag + "-" + randomSuffix;
        String batchLot = "LOT-" + dateTag + "-" + randomSuffix;

        ProductionBatch batch = new ProductionBatch();
        batch.setBatchNumber(batchNumber);
        batch.setRecipe(recipe);
        batch.setTargetProductSku(targetSku);
        batch.setTargetProductName(targetName);
        batch.setPlannedQuantity(request.getPlannedQuantity());
        batch.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
        batch.setStatus(BatchStatus.PLANNED);
        batch.setPlannedStartDate(request.getPlannedStartDate() != null ? request.getPlannedStartDate() : Instant.now());
        batch.setSupervisor(request.getSupervisor());
        batch.setBatchLot(batchLot);
        batch.setNotes(request.getNotes());
        batch.setActualQuantity(BigDecimal.ZERO);
        batch.setTotalWastageQuantity(BigDecimal.ZERO);
        batch.setCreatedAt(Instant.now());
        batch.setUpdatedAt(Instant.now());

        ProductionBatch saved = batchRepository.save(batch);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public MaterialAvailabilityDto checkMaterialAvailability(UUID batchId) {
        ProductionBatch batch = getBatchEntity(batchId);
        Recipe recipe = batch.getRecipe();

        List<MaterialAvailabilityItemDto> items = new ArrayList<>();
        boolean allAvailable = true;

        if (recipe != null && recipe.getItems() != null && !recipe.getItems().isEmpty()) {
            BigDecimal scaleRatio = BigDecimal.ONE;
            if (recipe.getStandardBatchSize() != null && recipe.getStandardBatchSize().compareTo(BigDecimal.ZERO) > 0) {
                scaleRatio = batch.getPlannedQuantity().divide(recipe.getStandardBatchSize(), 6, RoundingMode.HALF_UP);
            }

            for (RecipeItem item : recipe.getItems()) {
                BigDecimal requiredQty = item.getRequiredQuantity().multiply(scaleRatio).setScale(3, RoundingMode.HALF_UP);
                String sku = item.getRawMaterialSku();

                Optional<InventoryStockClient.InventoryItemSummary> stockOpt = inventoryStockClient.getItemBySku(sku);
                BigDecimal currentStock = stockOpt.map(InventoryStockClient.InventoryItemSummary::getCurrentQuantity).orElse(BigDecimal.ZERO);
                BigDecimal allocatedStock = BigDecimal.ZERO; // Current baseline allocated
                BigDecimal availableStock = currentStock.subtract(allocatedStock);

                boolean sufficient = availableStock.compareTo(requiredQty) >= 0;
                BigDecimal shortfall = sufficient ? BigDecimal.ZERO : requiredQty.subtract(availableStock);

                if (!sufficient) {
                    allAvailable = false;
                }

                items.add(new MaterialAvailabilityItemDto(
                        sku,
                        item.getRawMaterialName(),
                        requiredQty,
                        currentStock,
                        allocatedStock,
                        availableStock,
                        item.getUnitOfMeasure(),
                        sufficient,
                        shortfall
                ));
            }
        }

        return new MaterialAvailabilityDto(batch.getId(), batch.getBatchNumber(), allAvailable, items);
    }

    @Transactional
    public ProductionBatchDto updateBatchStatus(UUID batchId, UpdateBatchStatusRequest request) {
        ProductionBatch batch = getBatchEntity(batchId);
        BatchStatus current = batch.getStatus();
        BatchStatus target = request.getStatus();

        if (current == target) {
            return mapToDto(batch);
        }

        validateStateTransition(current, target);

        // State-specific actions
        switch (target) {
            case MATERIALS_READY -> {
                // Verified materials ready
            }
            case IN_PRODUCTION -> {
                if (batch.getActualStartDate() == null) {
                    batch.setActualStartDate(Instant.now());
                }
            }
            case QUALITY_CHECK -> {
                // Under inspection
            }
            case COMPLETED -> {
                if (batch.getActualQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("Cannot complete batch " + batch.getBatchNumber() + " with 0 actual output quantity recorded.");
                }
                batch.setCompletionDate(Instant.now());
                if (batch.getPlannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal yield = batch.getActualQuantity()
                            .divide(batch.getPlannedQuantity(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                    batch.setYieldPercentage(yield);
                }
            }
            case CANCELLED -> {
                // Cancelled batch
            }
            default -> {}
        }

        batch.setStatus(target);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            String existingNotes = batch.getNotes() == null ? "" : batch.getNotes() + "\n";
            batch.setNotes(existingNotes + "[" + target + "] " + request.getNotes());
        }
        batch.setUpdatedAt(Instant.now());

        ProductionBatch saved = batchRepository.save(batch);
        return mapToDto(saved);
    }

    @Transactional
    public ProductionBatchDto recordConsumption(UUID batchId, BatchConsumptionRequest request) {
        ProductionBatch batch = getBatchEntity(batchId);

        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot consume materials for batch in status: " + batch.getStatus());
        }

        String sku = request.getRawMaterialSku().trim().toUpperCase(Locale.ROOT);
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (consumptionRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Duplicate consumption request with idempotency key: {}", idempotencyKey);
                return mapToDto(batch);
            }
        } else {
            idempotencyKey = "BATCH::" + batch.getBatchNumber() + "::CONSUMPTION::" + sku + "::" + UUID.randomUUID();
        }

        // Deduct raw material in inventory service
        inventoryStockClient.consumeRawMaterial(
                sku,
                request.getConsumedQuantity(),
                batch.getBatchNumber(),
                request.getActor(),
                request.getNotes(),
                idempotencyKey
        );

        String matName = request.getRawMaterialName();
        if (matName == null || matName.isBlank()) {
            matName = sku;
        }

        BatchConsumption consumption = new BatchConsumption();
        consumption.setBatch(batch);
        consumption.setRawMaterialSku(sku);
        consumption.setRawMaterialName(matName);
        consumption.setConsumedQuantity(request.getConsumedQuantity());
        consumption.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
        consumption.setConsumedAt(Instant.now());
        consumption.setActor(request.getActor() != null ? request.getActor() : "PRODUCTION_MANAGER");
        consumption.setNotes(request.getNotes());
        consumption.setIdempotencyKey(idempotencyKey);

        consumptionRepository.save(consumption);
        batch.getConsumptions().add(consumption);
        batch.setUpdatedAt(Instant.now());

        // If batch was in PLANNED or MATERIALS_READY, automatically advance to IN_PRODUCTION
        if (batch.getStatus() == BatchStatus.PLANNED || batch.getStatus() == BatchStatus.MATERIALS_READY) {
            batch.setStatus(BatchStatus.IN_PRODUCTION);
            if (batch.getActualStartDate() == null) {
                batch.setActualStartDate(Instant.now());
            }
        }

        ProductionBatch saved = batchRepository.save(batch);
        return mapToDto(saved);
    }

    @Transactional
    public ProductionBatchDto recordOutput(UUID batchId, BatchOutputRequest request) {
        ProductionBatch batch = getBatchEntity(batchId);

        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot record output for batch in status: " + batch.getStatus());
        }

        String sku = request.getOutputProductSku() != null && !request.getOutputProductSku().isBlank()
                ? request.getOutputProductSku().trim().toUpperCase(Locale.ROOT)
                : batch.getTargetProductSku();
        String prodName = request.getOutputProductName() != null && !request.getOutputProductName().isBlank()
                ? request.getOutputProductName()
                : batch.getTargetProductName();
        String batchLot = request.getBatchLot() != null && !request.getBatchLot().isBlank()
                ? request.getBatchLot()
                : batch.getBatchLot();

        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (outputRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Duplicate output request with idempotency key: {}", idempotencyKey);
                return mapToDto(batch);
            }
        } else {
            idempotencyKey = "BATCH::" + batch.getBatchNumber() + "::OUTPUT::" + sku + "::" + UUID.randomUUID();
        }

        // Add output to inventory stock
        inventoryStockClient.recordOutput(
                sku,
                prodName,
                request.getQuantityProduced(),
                batch.getBatchNumber(),
                batchLot,
                request.getActor(),
                request.getNotes(),
                idempotencyKey
        );

        BatchOutput output = new BatchOutput();
        output.setBatch(batch);
        output.setOutputProductSku(sku);
        output.setOutputProductName(prodName);
        output.setQuantityProduced(request.getQuantityProduced());
        output.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
        output.setBatchLot(batchLot);
        output.setQualityGrade(request.getQualityGrade() != null ? request.getQualityGrade() : "A_GRADE");
        output.setProducedAt(Instant.now());
        output.setActor(request.getActor() != null ? request.getActor() : "PRODUCTION_MANAGER");
        output.setNotes(request.getNotes());
        output.setIdempotencyKey(idempotencyKey);

        outputRepository.save(output);
        batch.getOutputs().add(output);

        // Update actual quantity
        BigDecimal newActual = batch.getActualQuantity().add(request.getQuantityProduced());
        batch.setActualQuantity(newActual);

        // Compute yield percentage
        if (batch.getPlannedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal yield = newActual.divide(batch.getPlannedQuantity(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            batch.setYieldPercentage(yield);
        }

        batch.setUpdatedAt(Instant.now());
        ProductionBatch saved = batchRepository.save(batch);
        return mapToDto(saved);
    }

    @Transactional
    public ProductionBatchDto recordWastage(UUID batchId, BatchWastageRequest request) {
        ProductionBatch batch = getBatchEntity(batchId);

        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot record wastage for batch in status: " + batch.getStatus());
        }

        String sku = request.getMaterialSku().trim().toUpperCase(Locale.ROOT);
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (wastageRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Duplicate wastage request with idempotency key: {}", idempotencyKey);
                return mapToDto(batch);
            }
        } else {
            idempotencyKey = "BATCH::" + batch.getBatchNumber() + "::WASTAGE::" + sku + "::" + UUID.randomUUID();
        }

        // Record loss in inventory
        inventoryStockClient.recordWastage(
                sku,
                request.getWastageQuantity(),
                batch.getBatchNumber(),
                request.getWastageType(),
                request.getActor(),
                request.getReason(),
                idempotencyKey
        );

        String matName = request.getMaterialName();
        if (matName == null || matName.isBlank()) {
            matName = sku;
        }

        BatchWastage wastage = new BatchWastage();
        wastage.setBatch(batch);
        wastage.setMaterialSku(sku);
        wastage.setMaterialName(matName);
        wastage.setWastageQuantity(request.getWastageQuantity());
        wastage.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
        wastage.setReason(request.getReason());
        wastage.setWastageType(request.getWastageType() != null ? request.getWastageType() : "SCRAP");
        wastage.setRecordedAt(Instant.now());
        wastage.setActor(request.getActor() != null ? request.getActor() : "PRODUCTION_MANAGER");
        wastage.setIdempotencyKey(idempotencyKey);

        wastageRepository.save(wastage);
        batch.getWastages().add(wastage);

        // Update total wastage quantity
        batch.setTotalWastageQuantity(batch.getTotalWastageQuantity().add(request.getWastageQuantity()));
        batch.setUpdatedAt(Instant.now());

        ProductionBatch saved = batchRepository.save(batch);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductionBatchDto> getAllBatches(BatchStatus status) {
        List<ProductionBatch> batches;
        if (status != null) {
            batches = batchRepository.findByStatus(status);
        } else {
            batches = batchRepository.findAll();
        }
        return batches.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductionBatchDto getBatchById(UUID batchId) {
        return mapToDto(getBatchEntity(batchId));
    }

    @Transactional(readOnly = true)
    public ProductionBatchDto getBatchByNumber(String batchNumber) {
        ProductionBatch batch = batchRepository.findByBatchNumberIgnoreCase(batchNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Production batch not found for batch number: " + batchNumber));
        return mapToDto(batch);
    }

    private ProductionBatch getBatchEntity(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Production batch not found for ID: " + batchId));
    }

    private void validateStateTransition(BatchStatus current, BatchStatus target) {
        if (current == BatchStatus.COMPLETED) {
            throw new IllegalStateException("Cannot change status of a COMPLETED batch.");
        }
        if (current == BatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change status of a CANCELLED batch.");
        }

        if (target == BatchStatus.CANCELLED) {
            return; // Can cancel from any active state
        }

        boolean valid = switch (current) {
            case PLANNED -> target == BatchStatus.MATERIALS_READY || target == BatchStatus.IN_PRODUCTION;
            case MATERIALS_READY -> target == BatchStatus.IN_PRODUCTION;
            case IN_PRODUCTION -> target == BatchStatus.QUALITY_CHECK || target == BatchStatus.COMPLETED;
            case QUALITY_CHECK -> target == BatchStatus.COMPLETED;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid state transition from " + current + " to " + target + " for production batch.");
        }
    }

    public ProductionBatchDto mapToDto(ProductionBatch batch) {
        ProductionBatchDto dto = new ProductionBatchDto();
        dto.setId(batch.getId());
        dto.setBatchNumber(batch.getBatchNumber());
        if (batch.getRecipe() != null) {
            dto.setRecipeId(batch.getRecipe().getId());
            dto.setRecipeCode(batch.getRecipe().getRecipeCode());
            dto.setRecipeName(batch.getRecipe().getRecipeName());
        }
        dto.setTargetProductSku(batch.getTargetProductSku());
        dto.setTargetProductName(batch.getTargetProductName());
        dto.setPlannedQuantity(batch.getPlannedQuantity());
        dto.setActualQuantity(batch.getActualQuantity());
        dto.setUnitOfMeasure(batch.getUnitOfMeasure());
        dto.setStatus(batch.getStatus());
        dto.setPlannedStartDate(batch.getPlannedStartDate());
        dto.setActualStartDate(batch.getActualStartDate());
        dto.setCompletionDate(batch.getCompletionDate());
        dto.setSupervisor(batch.getSupervisor());
        dto.setBatchLot(batch.getBatchLot());
        dto.setNotes(batch.getNotes());
        dto.setTotalWastageQuantity(batch.getTotalWastageQuantity());
        dto.setYieldPercentage(batch.getYieldPercentage());
        dto.setCreatedAt(batch.getCreatedAt());
        dto.setUpdatedAt(batch.getUpdatedAt());

        if (batch.getConsumptions() != null) {
            dto.setConsumptions(new ArrayList<>(batch.getConsumptions()));
        }
        if (batch.getOutputs() != null) {
            dto.setOutputs(new ArrayList<>(batch.getOutputs()));
        }
        if (batch.getWastages() != null) {
            dto.setWastages(new ArrayList<>(batch.getWastages()));
        }

        return dto;
    }
}
