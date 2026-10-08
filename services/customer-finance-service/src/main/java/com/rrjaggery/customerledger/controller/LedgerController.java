package com.rrjaggery.customerledger.controller;

import com.rrjaggery.customerledger.config.CustomerLedgerSecurityUtils;
import com.rrjaggery.customerledger.dto.CustomerPaymentDto;
import com.rrjaggery.customerledger.dto.CustomerStatementDto;
import com.rrjaggery.customerledger.dto.RecordPaymentRequest;
import com.rrjaggery.customerledger.service.LedgerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class LedgerController {

    private final LedgerService ledgerService;
    private final CustomerLedgerSecurityUtils securityUtils;

    public LedgerController(LedgerService ledgerService, CustomerLedgerSecurityUtils securityUtils) {
        this.ledgerService = ledgerService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/{customerId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CustomerPaymentDto> recordPayment(
            @PathVariable UUID customerId,
            @Valid @RequestBody RecordPaymentRequest req,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        CustomerPaymentDto payment = ledgerService.recordPayment(customerId, req, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{customerId}/ledger")
    public ResponseEntity<CustomerStatementDto> getCustomerStatement(
            @PathVariable UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        if (!user.isAdminOrManager() && !customerId.equals(user.getUserId())) {
            throw new SecurityException("Cannot access ledger statement of another customer.");
        }
        CustomerStatementDto statement = ledgerService.getCustomerStatement(customerId, from, to);
        return ResponseEntity.ok(statement);
    }

    @GetMapping("/{customerId}/payments")
    public ResponseEntity<List<CustomerPaymentDto>> getCustomerPayments(
            @PathVariable UUID customerId,
            HttpServletRequest request) {
        CustomerLedgerSecurityUtils.AuthenticatedUser user = securityUtils.getAuthenticatedUser(request);
        if (!user.isAdminOrManager() && !customerId.equals(user.getUserId())) {
            throw new SecurityException("Cannot access payment history of another customer.");
        }
        List<CustomerPaymentDto> payments = ledgerService.getCustomerPayments(customerId);
        return ResponseEntity.ok(payments);
    }
}
