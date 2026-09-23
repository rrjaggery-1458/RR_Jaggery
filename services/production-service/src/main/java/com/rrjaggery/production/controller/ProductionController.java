package com.rrjaggery.production.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.production.dto.*;
import com.rrjaggery.production.entity.BatchStatus;
import com.rrjaggery.production.service.ProductionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/production/batches")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        ProductionBatchDto created = productionService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Production batch created successfully."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProductionBatchDto>>> getBatches(
            @RequestParam(required = false) BatchStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.getAllBatches(status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> getBatchById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.getBatchById(id)));
    }

    @GetMapping("/number/{batchNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> getBatchByNumber(@PathVariable String batchNumber) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.getBatchByNumber(batchNumber)));
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialAvailabilityDto>> checkMaterialAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.checkMaterialAvailability(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> updateBatchStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBatchStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.updateBatchStatus(id, request), "Batch status updated successfully."));
    }

    @PostMapping("/{id}/consume")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> recordConsumption(
            @PathVariable UUID id,
            @Valid @RequestBody BatchConsumptionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.recordConsumption(id, request), "Material consumption recorded successfully."));
    }

    @PostMapping("/{id}/output")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> recordOutput(
            @PathVariable UUID id,
            @Valid @RequestBody BatchOutputRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.recordOutput(id, request), "Production output recorded successfully."));
    }

    @PostMapping("/{id}/wastage")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<ProductionBatchDto>> recordWastage(
            @PathVariable UUID id,
            @Valid @RequestBody BatchWastageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productionService.recordWastage(id, request), "Production wastage recorded successfully."));
    }
}
