package com.rrjaggery.finance.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.finance.dto.FinancialBalanceDto;
import com.rrjaggery.finance.service.FinancialBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/finance/balance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class FinancialBalanceController {

    private final FinancialBalanceService financialBalanceService;

    public FinancialBalanceController(FinancialBalanceService financialBalanceService) {
        this.financialBalanceService = financialBalanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FinancialBalanceDto>> getFinancialBalance(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        FinancialBalanceDto balance = financialBalanceService.calculateFinancialBalance(year, month);
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }
}
