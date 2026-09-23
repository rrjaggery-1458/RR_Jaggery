package com.rrjaggery.customerledger.controller;

import com.rrjaggery.customerledger.config.CustomerLedgerSecurityUtils;
import com.rrjaggery.customerledger.dto.CreateCustomerRequest;
import com.rrjaggery.customerledger.dto.CustomerDto;
import com.rrjaggery.customerledger.dto.UpdateCustomerRequest;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.service.CustomerService;
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
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerLedgerSecurityUtils securityUtils;

    public CustomerController(CustomerService customerService, CustomerLedgerSecurityUtils securityUtils) {
        this.customerService = customerService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @RequestParam(required = false) CustomerType type,
            @RequestParam(required = false) String search) {
        List<CustomerDto> list = customerService.getAllCustomers(type, search);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CustomerDto> createCustomer(
            @Valid @RequestBody CreateCustomerRequest req,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        CustomerDto created = customerService.createCustomer(req, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(
            @PathVariable UUID id,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        CustomerDto customer = customerService.getCustomerById(id);
        if (!user.isAdminOrManager() && !customer.getId().equals(user.getUserId()) && (customer.getAuthUserId() == null || !customer.getAuthUserId().equals(user.getUserId()))) {
            throw new SecurityException("Cannot view customer profile of another user.");
        }
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest req) {
        CustomerDto updated = customerService.updateCustomer(id, req);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/profile/me")
    public ResponseEntity<CustomerDto> getMyCustomerProfile(HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        try {
            CustomerDto customer = customerService.getCustomerByAuthUserId(user.getUserId());
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
