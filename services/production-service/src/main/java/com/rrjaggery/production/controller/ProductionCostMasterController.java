package com.rrjaggery.production.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.production.entity.ProductionCostMaster;
import com.rrjaggery.production.repository.ProductionCostMasterRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/production/cost-masters")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ProductionCostMasterController {

    private final ProductionCostMasterRepository repository;

    public ProductionCostMasterController(ProductionCostMasterRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductionCostMaster>>> getAll(
            @RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(repository.findByActiveTrueOrderByCostNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(repository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionCostMaster>> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(cm -> ResponseEntity.ok(ApiResponse.ok(cm)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductionCostMaster>> create(@Valid @RequestBody ProductionCostMaster req) {
        if (req.getEffectiveFrom() == null) {
            req.setEffectiveFrom(LocalDate.now());
        }
        ProductionCostMaster saved = repository.save(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Production cost master created."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionCostMaster>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductionCostMaster req) {
        return repository.findById(id).map(existing -> {
            existing.setCostName(req.getCostName());
            existing.setCostType(req.getCostType());
            existing.setCalculationBasis(req.getCalculationBasis());
            existing.setUnit(req.getUnit());
            existing.setRate(req.getRate());
            existing.setEffectiveFrom(req.getEffectiveFrom());
            existing.setEffectiveTo(req.getEffectiveTo());
            existing.setActive(req.isActive());
            existing.setNotes(req.getNotes());
            ProductionCostMaster updated = repository.save(existing);
            return ResponseEntity.ok(ApiResponse.ok(updated, "Production cost master updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable UUID id) {
        return repository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            repository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Production cost master status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
