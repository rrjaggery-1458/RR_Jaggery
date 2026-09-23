package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.config.CustomerSecurityUtils;
import com.rrjaggery.commerce.dto.AddToCartRequest;
import com.rrjaggery.commerce.dto.CartDto;
import com.rrjaggery.commerce.dto.CartItemDto;
import com.rrjaggery.commerce.dto.UpdateCartItemRequest;
import com.rrjaggery.commerce.service.CartService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commerce/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;
    private final CustomerSecurityUtils securityUtils;

    public CartController(CartService cartService, CustomerSecurityUtils securityUtils) {
        this.cartService = cartService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDto>> getCart(HttpServletRequest request) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        CartDto cart = cartService.getCart(customer.getUserId(), customer.getCustomerType());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemDto>> addToCart(
            @Valid @RequestBody AddToCartRequest req,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        CartItemDto item = cartService.addToCart(customer.getUserId(), req, customer.getCustomerType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(item));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartItemDto>> updateQuantity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemRequest req,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        CartItemDto updated = cartService.updateQuantity(customer.getUserId(), id, req.getQuantity(), customer.getCustomerType());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        cartService.removeItem(customer.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(HttpServletRequest request) {
        CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
        cartService.clearCart(customer.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
