package com.rrjaggery.customerledger.controller;

import com.rrjaggery.customerledger.config.CustomerLedgerSecurityUtils;
import com.rrjaggery.customerledger.dto.CreateOfflineOrderRequest;
import com.rrjaggery.customerledger.dto.OfflineInvoiceDto;
import com.rrjaggery.customerledger.dto.OfflineOrderDto;
import com.rrjaggery.customerledger.service.OfflineOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class OfflineOrderController {

    private final OfflineOrderService offlineOrderService;
    private final CustomerLedgerSecurityUtils securityUtils;

    public OfflineOrderController(OfflineOrderService offlineOrderService, CustomerLedgerSecurityUtils securityUtils) {
        this.offlineOrderService = offlineOrderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/offline-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<OfflineOrderDto> createOfflineOrder(
            @Valid @RequestBody CreateOfflineOrderRequest req,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/offline-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<OfflineOrderDto>> getAllOfflineOrders() {
        List<OfflineOrderDto> orders = offlineOrderService.getAllOfflineOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/offline-orders/{orderId}")
    public ResponseEntity<OfflineOrderDto> getOfflineOrderById(
            @PathVariable UUID orderId,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        OfflineOrderDto order = offlineOrderService.getOfflineOrderById(orderId);
        if (!user.isAdminOrManager() && !order.getCustomerId().equals(user.getUserId())) {
            throw new SecurityException("Cannot access offline order of another customer.");
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/offline-orders/{orderId}/invoice")
    public ResponseEntity<OfflineInvoiceDto> getOfflineInvoice(
            @PathVariable UUID orderId,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        OfflineInvoiceDto invoice = offlineOrderService.getOfflineInvoice(orderId);
        if (!user.isAdminOrManager() && !invoice.getCustomerId().equals(user.getUserId())) {
            throw new SecurityException("Cannot access offline invoice of another customer.");
        }
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{customerId}/offline-orders")
    public ResponseEntity<List<OfflineOrderDto>> getCustomerOfflineOrders(
            @PathVariable UUID customerId,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        if (!user.isAdminOrManager() && !customerId.equals(user.getUserId())) {
            throw new SecurityException("Cannot view orders of another customer.");
        }
        List<OfflineOrderDto> orders = offlineOrderService.getCustomerOfflineOrders(customerId);
        return ResponseEntity.ok(orders);
    }
}
