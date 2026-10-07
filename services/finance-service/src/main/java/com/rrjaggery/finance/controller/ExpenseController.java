package com.rrjaggery.finance.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.finance.dto.CategoryExpenseDto;
import com.rrjaggery.finance.dto.CreateExpenseRequest;
import com.rrjaggery.finance.dto.UpdateExpenseRequest;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/finance/expenses")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Expense>>> getExpenses(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if ((year == null || month == null) && startDate != null) {
            year = startDate.getYear();
            month = startDate.getMonthValue();
        }
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenses(year, month, category)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenseById(id)));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<ApiResponse<List<CategoryExpenseDto>>> getBreakdown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if ((year == null || month == null) && startDate != null) {
            year = startDate.getYear();
            month = startDate.getMonthValue();
        }
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenseBreakdown(year, month)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        Expense created = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created, "Expense recorded successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        Expense updated = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Expense updated successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Expense deleted successfully."));
    }
}
