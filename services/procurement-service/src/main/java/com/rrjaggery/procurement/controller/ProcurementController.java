package com.rrjaggery.procurement.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.procurement.dto.CreatePurchaseOrderRequest;
import com.rrjaggery.procurement.dto.CreateSupplierRequest;
import com.rrjaggery.procurement.dto.ReceiveGoodsRequest;
import com.rrjaggery.procurement.entity.GoodsReceipt;
import com.rrjaggery.procurement.entity.PurchaseOrder;
import com.rrjaggery.procurement.entity.Supplier;
import com.rrjaggery.procurement.entity.SupplierLedgerEntry;
import com.rrjaggery.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/procurement")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(procurementService.createSupplier(request), "Supplier created."));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<Supplier>>> getSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getSuppliers()));
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Supplier>> getSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getSupplier(id)));
    }

    @GetMapping("/suppliers/{id}/ledger")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<SupplierLedgerEntry>>> getSupplierLedger(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getSupplierLedger(id)));
    }

    @GetMapping("/suppliers/{id}/outstanding")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getSupplierOutstanding(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getSupplierOutstanding(id)));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchaseOrder(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(procurementService.createPurchaseOrder(request), "Purchase order created."));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getPurchaseOrders() {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getPurchaseOrders()));
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> getPurchaseOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getPurchaseOrder(id)));
    }

    @PostMapping("/goods-receipts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> receiveGoods(@Valid @RequestBody ReceiveGoodsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.receiveGoods(request), "Goods receipt recorded."));
    }

    @GetMapping("/purchase-orders/{id}/receipts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<GoodsReceipt>>> getReceipts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(procurementService.getReceipts(id)));
    }
}
