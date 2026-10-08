package com.rrjaggery.finance.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.finance.dto.*;
import com.rrjaggery.finance.entity.*;
import com.rrjaggery.finance.service.PayrollService;
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
@RequestMapping("/api/v1/finance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // ===== EMPLOYEES / WORKERS =====

    @GetMapping({"/employees", "/workers"})
    public ResponseEntity<ApiResponse<List<Employee>>> getEmployees(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        if (Boolean.FALSE.equals(activeOnly)) {
            return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllEmployees()));
        }
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getActiveEmployees()));
    }

    @GetMapping({"/employees/{id}", "/workers/{id}"})
    public ResponseEntity<ApiResponse<Employee>> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getEmployee(id)));
    }

    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<Employee>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        Employee created = payrollService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created, "Employee created successfully."));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        Employee updated = payrollService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Employee updated successfully."));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        payrollService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Employee deleted successfully."));
    }

    // ===== ATTENDANCE =====

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<Attendance>>> getAttendance(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate start = startDate;
        LocalDate end = endDate;
        if (start == null && year != null && month != null) {
            java.time.YearMonth ym = java.time.YearMonth.of(year, month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        } else if (start == null) {
            start = LocalDate.now().withDayOfMonth(1);
            end = LocalDate.now();
        }

        if (employeeId != null) {
            return ResponseEntity.ok(ApiResponse.ok(payrollService.getEmployeeAttendance(employeeId, start, end)));
        }
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAttendanceForPeriod(start, end)));
    }

    @PostMapping("/attendance")
    public ResponseEntity<ApiResponse<Attendance>> recordAttendance(@Valid @RequestBody RecordAttendanceRequest request) {
        Attendance recorded = payrollService.recordAttendance(request);
        return ResponseEntity.ok(ApiResponse.ok(recorded, "Attendance recorded successfully."));
    }

    // ===== PAYROLL CALCULATION =====

    @GetMapping("/payroll/calculate")
    public ResponseEntity<ApiResponse<PayrollCalculationResult>> calculatePayroll(
            @RequestParam UUID employeeId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) java.math.BigDecimal batchesWorked,
            @RequestParam(required = false) java.math.BigDecimal quantityKg) {
        PayrollCalculationResult result = payrollService.calculatePayroll(employeeId, year, month, batchesWorked, quantityKg);
        return ResponseEntity.ok(ApiResponse.ok(result, "Payroll calculation complete."));
    }

    // ===== PAYROLL RECORDS =====

    @GetMapping("/payroll")
    public ResponseEntity<ApiResponse<List<PayrollRecord>>> getPayroll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayrollRecords(year, month)));
    }

    @PostMapping("/payroll")
    public ResponseEntity<ApiResponse<PayrollRecord>> createPayroll(@Valid @RequestBody CreatePayrollRequest request) {
        PayrollRecord created = payrollService.createPayrollRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created, "Payroll payment recorded successfully."));
    }

    // ===== SALARY ADVANCES =====

    @GetMapping("/salary-advances")
    public ResponseEntity<ApiResponse<List<SalaryAdvance>>> getAdvances(
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAdvances(employeeId)));
    }

    @PostMapping("/salary-advances")
    public ResponseEntity<ApiResponse<SalaryAdvance>> createAdvance(@Valid @RequestBody CreateSalaryAdvanceRequest request) {
        SalaryAdvance advance = payrollService.createAdvance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(advance, "Salary advance recorded successfully."));
    }

    // ===== SALARY DEDUCTIONS =====

    @GetMapping("/salary-deductions")
    public ResponseEntity<ApiResponse<List<SalaryDeduction>>> getDeductions(
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getDeductions(employeeId)));
    }

    @PostMapping("/salary-deductions")
    public ResponseEntity<ApiResponse<SalaryDeduction>> createDeduction(@Valid @RequestBody CreateSalaryDeductionRequest request) {
        SalaryDeduction deduction = payrollService.createDeduction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(deduction, "Deduction recorded successfully."));
    }

    // ===== SALARY PAYMENTS =====

    @GetMapping("/salary-payments")
    public ResponseEntity<ApiResponse<List<SalaryPayment>>> getPayments(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayments(employeeId, year, month)));
    }

    @PostMapping("/salary-payments")
    public ResponseEntity<ApiResponse<SalaryPayment>> recordSalaryPayment(@Valid @RequestBody RecordSalaryPaymentRequest request) {
        SalaryPayment payment = payrollService.recordSalaryPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(payment, "Salary payment recorded successfully."));
    }

    // ===== EMPLOYEE LEDGER =====

    @GetMapping("/employees/{id}/ledger")
    public ResponseEntity<ApiResponse<List<EmployeeLedgerEntry>>> getEmployeeLedger(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getEmployeeLedger(id)));
    }

    @GetMapping("/employee-ledger")
    public ResponseEntity<ApiResponse<List<EmployeeLedgerEntry>>> getAllLedger() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllLedgerEntries()));
    }
}
