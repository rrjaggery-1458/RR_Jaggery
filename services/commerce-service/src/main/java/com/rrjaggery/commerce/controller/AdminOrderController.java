package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.dto.OrderDto;
import com.rrjaggery.commerce.dto.UpdateOrderStatusRequest;
import com.rrjaggery.commerce.entity.OrderStatus;
import com.rrjaggery.commerce.service.OrderService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commerce/admin/orders")
@CrossOrigin(origins = "*")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status
    ) {
        List<OrderDto> orders = orderService.getAllOrdersAdmin(status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable UUID id) {
        OrderDto order = orderService.getOrderById(id, null, true);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        OrderDto updated = orderService.updateOrderStatus(id, req.getStatus());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
