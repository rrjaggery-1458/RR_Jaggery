package com.rrjaggery.finance.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.finance.entity.*;
import com.rrjaggery.finance.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/finance/business-masters")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BusinessMasterController {

    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final DeductionTypeRepository deductionTypeRepository;
    private final AdvanceTypeRepository advanceTypeRepository;
    private final PayrollRuleRepository payrollRuleRepository;

    public BusinessMasterController(
            ExpenseCategoryRepository categoryRepository,
            PaymentMethodRepository paymentMethodRepository,
            DeductionTypeRepository deductionTypeRepository,
            AdvanceTypeRepository advanceTypeRepository,
            PayrollRuleRepository payrollRuleRepository) {
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.deductionTypeRepository = deductionTypeRepository;
        this.advanceTypeRepository = advanceTypeRepository;
        this.payrollRuleRepository = payrollRuleRepository;
    }

    // ── 1. Expense Categories ──────────────────────────────────────────────────
    @GetMapping("/expense-categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategory>>> getExpenseCategories(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(categoryRepository.findByActiveTrueOrderByNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(categoryRepository.findAll()));
    }

    @PostMapping("/expense-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseCategory>> createExpenseCategory(@Valid @RequestBody ExpenseCategory cat) {
        ExpenseCategory saved = categoryRepository.save(cat);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Expense category created."));
    }

    @PutMapping("/expense-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseCategory>> updateExpenseCategory(@PathVariable UUID id, @Valid @RequestBody ExpenseCategory req) {
        return categoryRepository.findById(id).map(existing -> {
            existing.setName(req.getName());
            existing.setDescription(req.getDescription());
            existing.setParentCategory(req.getParentCategory());
            existing.setActive(req.isActive());
            existing.setNotes(req.getNotes());
            return ResponseEntity.ok(ApiResponse.ok(categoryRepository.save(existing), "Category updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/expense-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleExpenseCategory(@PathVariable UUID id) {
        return categoryRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            categoryRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Category status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── 2. Payment Methods ─────────────────────────────────────────────────────
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethod>>> getPaymentMethods(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(paymentMethodRepository.findByActiveTrueOrderByDisplayNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(paymentMethodRepository.findAll()));
    }

    @PostMapping("/payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentMethod>> createPaymentMethod(@Valid @RequestBody PaymentMethod pm) {
        PaymentMethod saved = paymentMethodRepository.save(pm);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Payment method created."));
    }

    @PutMapping("/payment-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentMethod>> updatePaymentMethod(@PathVariable UUID id, @Valid @RequestBody PaymentMethod req) {
        return paymentMethodRepository.findById(id).map(existing -> {
            existing.setCode(req.getCode());
            existing.setDisplayName(req.getDisplayName());
            existing.setActive(req.isActive());
            existing.setNotes(req.getNotes());
            return ResponseEntity.ok(ApiResponse.ok(paymentMethodRepository.save(existing), "Payment method updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/payment-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> togglePaymentMethod(@PathVariable UUID id) {
        return paymentMethodRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            paymentMethodRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Payment method status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── 3. Deduction Types ─────────────────────────────────────────────────────
    @GetMapping("/deduction-types")
    public ResponseEntity<ApiResponse<List<DeductionType>>> getDeductionTypes(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(deductionTypeRepository.findByActiveTrueOrderByNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(deductionTypeRepository.findAll()));
    }

    @PostMapping("/deduction-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeductionType>> createDeductionType(@Valid @RequestBody DeductionType dt) {
        DeductionType saved = deductionTypeRepository.save(dt);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Deduction type created."));
    }

    @PutMapping("/deduction-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeductionType>> updateDeductionType(@PathVariable UUID id, @Valid @RequestBody DeductionType req) {
        return deductionTypeRepository.findById(id).map(existing -> {
            existing.setCode(req.getCode());
            existing.setName(req.getName());
            existing.setDescription(req.getDescription());
            existing.setActive(req.isActive());
            return ResponseEntity.ok(ApiResponse.ok(deductionTypeRepository.save(existing), "Deduction type updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/deduction-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleDeductionType(@PathVariable UUID id) {
        return deductionTypeRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            deductionTypeRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Deduction type status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── 4. Advance Types ───────────────────────────────────────────────────────
    @GetMapping("/advance-types")
    public ResponseEntity<ApiResponse<List<AdvanceType>>> getAdvanceTypes(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(advanceTypeRepository.findByActiveTrueOrderByNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(advanceTypeRepository.findAll()));
    }

    @PostMapping("/advance-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdvanceType>> createAdvanceType(@Valid @RequestBody AdvanceType at) {
        AdvanceType saved = advanceTypeRepository.save(at);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Advance type created."));
    }

    @PutMapping("/advance-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdvanceType>> updateAdvanceType(@PathVariable UUID id, @Valid @RequestBody AdvanceType req) {
        return advanceTypeRepository.findById(id).map(existing -> {
            existing.setCode(req.getCode());
            existing.setName(req.getName());
            existing.setDescription(req.getDescription());
            existing.setActive(req.isActive());
            return ResponseEntity.ok(ApiResponse.ok(advanceTypeRepository.save(existing), "Advance type updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/advance-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleAdvanceType(@PathVariable UUID id) {
        return advanceTypeRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            advanceTypeRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Advance type status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── 5. Payroll Rules ───────────────────────────────────────────────────────
    @GetMapping("/payroll-rules")
    public ResponseEntity<ApiResponse<List<PayrollRule>>> getPayrollRules(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(payrollRuleRepository.findByActiveTrueOrderByRuleNameAsc()));
        }
        return ResponseEntity.ok(ApiResponse.ok(payrollRuleRepository.findAll()));
    }

    @PostMapping("/payroll-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRule>> createPayrollRule(@Valid @RequestBody PayrollRule rule) {
        if (rule.getEffectiveFrom() == null) {
            rule.setEffectiveFrom(LocalDate.now());
        }
        PayrollRule saved = payrollRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved, "Payroll rule created."));
    }

    @PutMapping("/payroll-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRule>> updatePayrollRule(@PathVariable UUID id, @Valid @RequestBody PayrollRule req) {
        return payrollRuleRepository.findById(id).map(existing -> {
            existing.setRuleName(req.getRuleName());
            existing.setStandardWorkingDays(req.getStandardWorkingDays());
            existing.setStandardHoursPerDay(req.getStandardHoursPerDay());
            existing.setOvertimeMultiplier(req.getOvertimeMultiplier());
            existing.setHalfDayFactor(req.getHalfDayFactor());
            existing.setEffectiveFrom(req.getEffectiveFrom());
            existing.setEffectiveTo(req.getEffectiveTo());
            existing.setActive(req.isActive());
            existing.setNotes(req.getNotes());
            return ResponseEntity.ok(ApiResponse.ok(payrollRuleRepository.save(existing), "Payroll rule updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/payroll-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> togglePayrollRule(@PathVariable UUID id) {
        return payrollRuleRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            payrollRuleRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Payroll rule status toggled."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
