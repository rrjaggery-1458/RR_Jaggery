package com.rrjaggery.inventory.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.inventory.dto.CreateInventoryItemRequest;
import com.rrjaggery.inventory.dto.ReconcileInventoryRequest;
import com.rrjaggery.inventory.dto.ReceiveStockRequest;
import com.rrjaggery.inventory.dto.SaleStockRequest;
import com.rrjaggery.inventory.dto.StockAdjustmentRequest;
import com.rrjaggery.inventory.entity.InventoryItem;
import com.rrjaggery.inventory.entity.StockMovement;
import com.rrjaggery.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(inventoryService.createItem(request), "Inventory item created."));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getItems() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getItems()));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> getItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getItem(id)));
    }

    @GetMapping("/items/sku/{sku}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> getItemBySku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getItemBySku(sku)));
    }

    @PostMapping("/stock/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.adjustStock(request), "Stock adjusted."));
    }

    @PostMapping("/stock/receive")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> receiveStock(@Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.receiveStock(request), "Stock received."));
    }

    @GetMapping("/items/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getMovements(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMovements(id)));
    }

    @GetMapping("/items/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockItems()));
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> reconcileStock(@Valid @RequestBody ReconcileInventoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.reconcileStock(request), "Inventory reconciliation completed."));
    }

    @PostMapping("/stock/sale")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<InventoryItem>> deductSaleStock(@Valid @RequestBody SaleStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.deductSaleStock(request), "Sale stock deducted."));
    }
}
