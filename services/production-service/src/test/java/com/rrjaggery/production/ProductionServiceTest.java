package com.rrjaggery.production;

import com.rrjaggery.production.dto.*;
import com.rrjaggery.production.entity.*;
import com.rrjaggery.production.repository.*;
import com.rrjaggery.production.service.InventoryStockClient;
import com.rrjaggery.production.service.ProductionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionServiceTest {

    @Mock
    private ProductionBatchRepository batchRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private BatchConsumptionRepository consumptionRepository;
    @Mock
    private BatchOutputRepository outputRepository;
    @Mock
    private BatchWastageRepository wastageRepository;
    @Mock
    private InventoryStockClient inventoryStockClient;

    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        productionService = new ProductionService(
                batchRepository,
                recipeRepository,
                consumptionRepository,
                outputRepository,
                wastageRepository,
                inventoryStockClient
        );
    }

    @Test
    void createBatch_Success() {
        CreateBatchRequest request = new CreateBatchRequest();
        request.setTargetProductSku("JAG-ORG-500G");
        request.setTargetProductName("Organic Jaggery 500g");
        request.setPlannedQuantity(new BigDecimal("100.00"));
        request.setSupervisor("Rohan S");

        when(batchRepository.save(any(ProductionBatch.class))).thenAnswer(invocation -> {
            ProductionBatch b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        ProductionBatchDto result = productionService.createBatch(request);

        assertNotNull(result);
        assertNotNull(result.getBatchNumber());
        assertTrue(result.getBatchNumber().startsWith("BATCH-"));
        assertEquals(BatchStatus.PLANNED, result.getStatus());
        assertEquals("JAG-ORG-500G", result.getTargetProductSku());
        assertEquals(new BigDecimal("100.00"), result.getPlannedQuantity());
        verify(batchRepository).save(any(ProductionBatch.class));
    }

    @Test
    void updateBatchStatus_ValidTransition_PLANNED_to_MATERIALS_READY() {
        UUID batchId = UUID.randomUUID();
        ProductionBatch batch = new ProductionBatch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-20260923-TEST01");
        batch.setStatus(BatchStatus.PLANNED);
        batch.setPlannedQuantity(new BigDecimal("100.00"));

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(ProductionBatch.class))).thenReturn(batch);

        UpdateBatchStatusRequest req = new UpdateBatchStatusRequest(BatchStatus.MATERIALS_READY, "SUPERVISOR", "Materials verified");
        ProductionBatchDto result = productionService.updateBatchStatus(batchId, req);

        assertEquals(BatchStatus.MATERIALS_READY, result.getStatus());
    }

    @Test
    void updateBatchStatus_InvalidTransition_ThrowsIllegalStateException() {
        UUID batchId = UUID.randomUUID();
        ProductionBatch batch = new ProductionBatch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-20260923-TEST02");
        batch.setStatus(BatchStatus.PLANNED);
        batch.setPlannedQuantity(new BigDecimal("100.00"));

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        // Attempt invalid jump from PLANNED directly to COMPLETED
        UpdateBatchStatusRequest req = new UpdateBatchStatusRequest(BatchStatus.COMPLETED, "SUPERVISOR", "Attempting invalid jump");
        assertThrows(IllegalStateException.class, () -> productionService.updateBatchStatus(batchId, req));
    }

    @Test
    void recordConsumption_Success() {
        UUID batchId = UUID.randomUUID();
        ProductionBatch batch = new ProductionBatch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-20260923-TEST03");
        batch.setStatus(BatchStatus.MATERIALS_READY);
        batch.setPlannedQuantity(new BigDecimal("100.00"));

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(consumptionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(batchRepository.save(any(ProductionBatch.class))).thenReturn(batch);

        BatchConsumptionRequest req = new BatchConsumptionRequest(
                "RAW-SUGARCANE-JUICE", "Raw Sugarcane Juice", new BigDecimal("120.00"), "KG", "Rohan S"
        );
        req.setIdempotencyKey("KEY-CONSUMPTION-1");

        ProductionBatchDto result = productionService.recordConsumption(batchId, req);

        assertEquals(BatchStatus.IN_PRODUCTION, result.getStatus());
        verify(inventoryStockClient).consumeRawMaterial(eq("RAW-SUGARCANE-JUICE"), eq(new BigDecimal("120.00")), eq("BATCH-20260923-TEST03"), any(), any(), any());
        verify(consumptionRepository).save(any(BatchConsumption.class));
    }

    @Test
    void recordOutput_CalculatesYieldCorrectly() {
        UUID batchId = UUID.randomUUID();
        ProductionBatch batch = new ProductionBatch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-20260923-TEST04");
        batch.setStatus(BatchStatus.IN_PRODUCTION);
        batch.setTargetProductSku("JAG-ORG-500G");
        batch.setPlannedQuantity(new BigDecimal("100.00"));
        batch.setActualQuantity(BigDecimal.ZERO);

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(outputRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(batchRepository.save(any(ProductionBatch.class))).thenReturn(batch);

        BatchOutputRequest req = new BatchOutputRequest(
                "JAG-ORG-500G", "Organic Jaggery 500g", new BigDecimal("96.00"), "KG", "LOT-TEST04", "A_GRADE", "Rohan S"
        );
        req.setIdempotencyKey("KEY-OUTPUT-1");

        ProductionBatchDto result = productionService.recordOutput(batchId, req);

        assertEquals(new BigDecimal("96.00"), result.getActualQuantity());
        assertEquals(new BigDecimal("96.00"), result.getYieldPercentage());
        verify(inventoryStockClient).recordOutput(eq("JAG-ORG-500G"), any(), eq(new BigDecimal("96.00")), eq("BATCH-20260923-TEST04"), any(), any(), any(), any());
    }

    @Test
    void recordWastage_Success() {
        UUID batchId = UUID.randomUUID();
        ProductionBatch batch = new ProductionBatch();
        batch.setId(batchId);
        batch.setBatchNumber("BATCH-20260923-TEST05");
        batch.setStatus(BatchStatus.IN_PRODUCTION);
        batch.setPlannedQuantity(new BigDecimal("100.00"));
        batch.setTotalWastageQuantity(BigDecimal.ZERO);

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(wastageRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(batchRepository.save(any(ProductionBatch.class))).thenReturn(batch);

        BatchWastageRequest req = new BatchWastageRequest(
                "RAW-SUGARCANE-JUICE", "Raw Sugarcane Juice", new BigDecimal("4.00"), "KG", "Boiling evaporation loss", "LOSS", "Rohan S"
        );
        req.setIdempotencyKey("KEY-WASTAGE-1");

        ProductionBatchDto result = productionService.recordWastage(batchId, req);

        assertEquals(new BigDecimal("4.00"), result.getTotalWastageQuantity());
        verify(inventoryStockClient).recordWastage(eq("RAW-SUGARCANE-JUICE"), eq(new BigDecimal("4.00")), eq("BATCH-20260923-TEST05"), eq("LOSS"), any(), any(), any());
    }
}
