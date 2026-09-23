package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.config.CustomerSecurityUtils;
import com.rrjaggery.commerce.dto.CheckoutRequest;
import com.rrjaggery.commerce.dto.InvoiceDto;
import com.rrjaggery.commerce.dto.OrderDto;
import com.rrjaggery.commerce.service.OrderService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commerce/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final CustomerSecurityUtils securityUtils;

    public OrderController(OrderService orderService, CustomerSecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderDto>> checkout(
            @Valid @RequestBody CheckoutRequest req,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        OrderDto order = orderService.checkout(
                customer.getUserId(),
                req.getShippingAddress().getRecipientName(),
                customer.getEmail(),
                req.getShippingAddress().getPhone(),
                customer.getCustomerType(),
                req
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getCustomerOrders(HttpServletRequest request) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        List<OrderDto> orders = orderService.getCustomerOrders(customer.getUserId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        OrderDto order = orderService.getOrderById(id, customer.getUserId(), customer.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        OrderDto cancelled = orderService.cancelOrder(id, customer.getUserId());
        return ResponseEntity.ok(ApiResponse.success(cancelled));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        InvoiceDto invoice = orderService.getInvoiceData(id, customer.getUserId(), customer.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
