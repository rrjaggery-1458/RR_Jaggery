package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.config.CustomerSecurityUtils;
import com.rrjaggery.commerce.dto.CheckoutRequest;
import com.rrjaggery.commerce.dto.InvoiceDto;
import com.rrjaggery.commerce.dto.OrderDto;
import com.rrjaggery.commerce.entity.PaymentMethod;
import com.rrjaggery.commerce.service.OrderService;
import com.rrjaggery.commerce.service.RazorpayPaymentService;
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
    private final RazorpayPaymentService razorpayService;

    public OrderController(OrderService orderService,
                           CustomerSecurityUtils securityUtils,
                           RazorpayPaymentService razorpayService) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderDto>> checkout(
            @Valid @RequestBody CheckoutRequest req,
            HttpServletRequest request
    ) {
        if (req.getPaymentMethod() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Payment method is required.", "MISSING_PAYMENT_METHOD"));
        }
        if (req.getPaymentMethod() == PaymentMethod.RAZORPAY) {
            if (!razorpayService.isEnabled()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Online payment via Razorpay is currently disabled.", "RAZORPAY_DISABLED"));
            }
            String rzpOrderId = req.getRazorpayOrderId();
            String rzpPaymentId = req.getRazorpayPaymentId();
            String rzpSignature = req.getRazorpaySignature();

            if (rzpOrderId == null || rzpOrderId.isBlank() ||
                rzpPaymentId == null || rzpPaymentId.isBlank() ||
                rzpSignature == null || rzpSignature.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Missing required Razorpay payment verification parameters.", "INVALID_PARAMETERS"));
            }

            boolean valid = razorpayService.verifyPaymentSignature(rzpOrderId, rzpPaymentId, rzpSignature);
            if (!valid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Payment verification failed. Invalid payment signature.", "SIGNATURE_MISMATCH"));
            }
        }

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
