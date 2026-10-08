package com.rrjaggery.finance.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.finance.dto.CreateInvestmentRequest;
import com.rrjaggery.finance.entity.Investment;
import com.rrjaggery.finance.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/investments")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(prefix = "app.forms", name = "enabled", havingValue = "true")
public class InvestmentController {
    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Investment>>> getInvestments() {
        return ResponseEntity.ok(ApiResponse.ok(investmentService.getInvestments()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Investment>> createInvestment(
            @Valid @RequestBody CreateInvestmentRequest request) {
        Investment created = investmentService.createInvestment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Investment recorded separately from operating transactions."));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/void")
    public ResponseEntity<ApiResponse<Investment>> voidInvestment(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        Investment voided = investmentService.voidInvestment(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(voided, "Investment voided successfully."));
    }
}
