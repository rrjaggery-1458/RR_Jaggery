package com.rrjaggery.finance.service;

import com.rrjaggery.finance.dto.*;
import com.rrjaggery.finance.entity.*;
import com.rrjaggery.finance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final SalaryDeductionRepository salaryDeductionRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeLedgerEntryRepository employeeLedgerEntryRepository;
    private final PayrollRuleRepository payrollRuleRepository;

    public PayrollService(EmployeeRepository employeeRepository,
                          AttendanceRepository attendanceRepository,
                          PayrollRecordRepository payrollRecordRepository,
                          SalaryAdvanceRepository salaryAdvanceRepository,
                          SalaryDeductionRepository salaryDeductionRepository,
                          SalaryPaymentRepository salaryPaymentRepository,
                          EmployeeLedgerEntryRepository employeeLedgerEntryRepository,
                          PayrollRuleRepository payrollRuleRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.salaryAdvanceRepository = salaryAdvanceRepository;
        this.salaryDeductionRepository = salaryDeductionRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.employeeLedgerEntryRepository = employeeLedgerEntryRepository;
        this.payrollRuleRepository = payrollRuleRepository;
    }

    // ===== EMPLOYEE MASTER =====

    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByActiveTrueOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    @Transactional
    public Employee createEmployee(CreateEmployeeRequest request) {
        String code = request.getEmployeeCode();
        if (code == null || code.isBlank()) {
            code = "EMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        } else {
            code = code.trim().toUpperCase(Locale.ROOT);
        }

        if (employeeRepository.existsByEmployeeCodeIgnoreCase(code)) {
            code = "EMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        }

        String wageType = request.getWageType() != null ? request.getWageType().trim().toUpperCase(Locale.ROOT) : "DAILY";
        if (!List.of("MONTHLY", "DAILY", "BATCH_BASED", "PER_KG", "CONTRACT").contains(wageType)) {
            throw new IllegalArgumentException("Invalid wage type: " + wageType + ". Must be one of: MONTHLY, DAILY, BATCH_BASED, PER_KG, CONTRACT");
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFullName(request.getFullName().trim());
        employee.setDesignation(request.getDesignation().trim());
        employee.setPhone(request.getPhone());
        employee.setWageType(wageType);
        employee.setRateAmount(request.getRateAmount() != null ? request.getRateAmount() : BigDecimal.ZERO);
        employee.setJoiningDate(request.getJoiningDate() != null ? request.getJoiningDate() : LocalDate.now());
        employee.setNotes(request.getNotes());
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(UUID id, UpdateEmployeeRequest request) {
        Employee employee = getEmployee(id);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            employee.setFullName(request.getFullName().trim());
        }
        if (request.getDesignation() != null && !request.getDesignation().isBlank()) {
            employee.setDesignation(request.getDesignation().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getWageType() != null && !request.getWageType().isBlank()) {
            String wt = request.getWageType().trim().toUpperCase(Locale.ROOT);
            if (!List.of("MONTHLY", "DAILY", "BATCH_BASED", "PER_KG", "CONTRACT").contains(wt)) {
                throw new IllegalArgumentException("Invalid wage type: " + wt);
            }
            employee.setWageType(wt);
        }
        if (request.getRateAmount() != null) {
            employee.setRateAmount(request.getRateAmount());
        }
        if (request.getJoiningDate() != null) {
            employee.setJoiningDate(request.getJoiningDate());
        }
        if (request.getNotes() != null) {
            employee.setNotes(request.getNotes());
        }
        employee.setActive(request.isActive());

        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        Employee employee = getEmployee(id);
        List<Attendance> attendances = attendanceRepository.findByEmployeeId(id);
        if (attendances != null && !attendances.isEmpty()) {
            attendanceRepository.deleteAll(attendances);
        }

        List<SalaryPayment> payments = salaryPaymentRepository.findByEmployeeIdOrderByPaymentDateDesc(id);
        if (payments != null && !payments.isEmpty()) {
            salaryPaymentRepository.deleteAll(payments);
        }

        List<EmployeeLedgerEntry> ledgers = employeeLedgerEntryRepository.findByEmployeeIdOrderByTransactionDateDescCreatedAtDesc(id);
        if (ledgers != null && !ledgers.isEmpty()) {
            employeeLedgerEntryRepository.deleteAll(ledgers);
        }

        employeeRepository.delete(employee);
    }

    // ===== ATTENDANCE =====

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceForPeriod(LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByAttendanceDateBetweenOrderByAttendanceDateDesc(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getEmployeeAttendance(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate);
    }

    @Transactional
    public Attendance recordAttendance(RecordAttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getEmployeeId()));

        // Find existing record for same employee+date, or create new (no duplicates)
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), request.getAttendanceDate())
                .orElse(new Attendance());

        attendance.setEmployeeId(employee.getId());
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        attendance.setOvertimeHours(request.getOvertimeHours() != null ? request.getOvertimeHours() : BigDecimal.ZERO);
        attendance.setNotes(request.getNotes());

        return attendanceRepository.save(attendance);
    }

    // ===== PAYROLL CALCULATION =====

    @Transactional(readOnly = true)
    public PayrollCalculationResult calculatePayroll(UUID employeeId, int periodYear, int periodMonth) {
        return calculatePayroll(employeeId, periodYear, periodMonth, null, null);
    }

    @Transactional(readOnly = true)
    public PayrollCalculationResult calculatePayroll(UUID employeeId, int periodYear, int periodMonth, BigDecimal batchesWorked, BigDecimal quantityKg) {
        Employee employee = getEmployee(employeeId);
        YearMonth ym = YearMonth.of(periodYear, periodMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // Gather attendance
        List<Attendance> attendanceList = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate);

        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal halfDays = BigDecimal.ZERO;
        BigDecimal absentDays = BigDecimal.ZERO;
        BigDecimal overtimeHoursTotal = BigDecimal.ZERO;

        for (Attendance a : attendanceList) {
            overtimeHoursTotal = overtimeHoursTotal.add(a.getOvertimeHours() != null ? a.getOvertimeHours() : BigDecimal.ZERO);
            switch (a.getStatus()) {
                case PRESENT -> presentDays = presentDays.add(BigDecimal.ONE);
                case HALF_DAY -> halfDays = halfDays.add(BigDecimal.ONE);
                case ABSENT -> absentDays = absentDays.add(BigDecimal.ONE);
            }
        }

        // Fetch configurable payroll rule for period
        LocalDate periodDate = LocalDate.of(periodYear, periodMonth, 1);
        List<PayrollRule> activeRules = payrollRuleRepository.findActiveRulesForDate(periodDate);
        BigDecimal stdMonthlyDays = new BigDecimal("26.00");
        BigDecimal hoursPerDay = new BigDecimal("8.00");
        BigDecimal otMultiplier = new BigDecimal("1.50");
        BigDecimal halfDayFactor = new BigDecimal("0.50");

        if (!activeRules.isEmpty()) {
            PayrollRule rule = activeRules.get(0);
            if (rule.getStandardWorkingDays() != null) stdMonthlyDays = rule.getStandardWorkingDays();
            if (rule.getStandardHoursPerDay() != null) hoursPerDay = rule.getStandardHoursPerDay();
            if (rule.getOvertimeMultiplier() != null) otMultiplier = rule.getOvertimeMultiplier();
            if (rule.getHalfDayFactor() != null) halfDayFactor = rule.getHalfDayFactor();
        }

        // Payable days = present + halfDayFactor * half_days
        BigDecimal payableDays = presentDays.add(halfDays.multiply(halfDayFactor));

        BigDecimal rateAmount = employee.getRateAmount() != null ? employee.getRateAmount() : BigDecimal.ZERO;
        BigDecimal baseEarned;
        BigDecimal overtimePay = BigDecimal.ZERO;

        // Wage-Type Safe Calculation & Overtime Applicability
        switch (employee.getWageType().toUpperCase(Locale.ROOT)) {
            case "MONTHLY" -> {
                // Base earned = (rate / stdMonthlyDays) * payable_days
                BigDecimal dailyRate = rateAmount.divide(stdMonthlyDays, 4, RoundingMode.HALF_UP);
                baseEarned = dailyRate.multiply(payableDays).setScale(2, RoundingMode.HALF_UP);
                // Overtime = (daily_rate / hoursPerDay) * otMultiplier * OT_hours
                BigDecimal hourlyRate = dailyRate.divide(hoursPerDay, 4, RoundingMode.HALF_UP);
                overtimePay = hourlyRate.multiply(otMultiplier).multiply(overtimeHoursTotal).setScale(2, RoundingMode.HALF_UP);
            }
            case "DAILY" -> {
                baseEarned = rateAmount.multiply(payableDays).setScale(2, RoundingMode.HALF_UP);
                BigDecimal hourlyRate = rateAmount.divide(hoursPerDay, 4, RoundingMode.HALF_UP);
                overtimePay = hourlyRate.multiply(otMultiplier).multiply(overtimeHoursTotal).setScale(2, RoundingMode.HALF_UP);
            }
            case "BATCH_BASED" -> {
                // Configurable batch rate calculation: rateAmount * (batchesWorked or payableDays)
                BigDecimal batches = (batchesWorked != null && batchesWorked.compareTo(BigDecimal.ZERO) > 0)
                        ? batchesWorked
                        : payableDays;
                baseEarned = rateAmount.multiply(batches).setScale(2, RoundingMode.HALF_UP);
                // Overtime explicitly NOT applicable for piece/batch workers
                overtimePay = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            case "PER_KG" -> {
                // Configurable per-KG output rate calculation: rateAmount * quantityKg
                BigDecimal kgQty = (quantityKg != null && quantityKg.compareTo(BigDecimal.ZERO) > 0)
                        ? quantityKg
                        : payableDays.multiply(new BigDecimal("100.00")); // default 100 kg per attended day if unassigned
                baseEarned = rateAmount.multiply(kgQty).setScale(2, RoundingMode.HALF_UP);
                // Overtime explicitly NOT applicable for piece-rate workers
                overtimePay = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            case "CONTRACT" -> {
                // Fixed lump sum for the period
                baseEarned = rateAmount.setScale(2, RoundingMode.HALF_UP);
                // Overtime explicitly NOT applicable for contract workers
                overtimePay = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            default -> {
                baseEarned = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                overtimePay = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        // Deductions for this period
        List<SalaryDeduction> deductionList = salaryDeductionRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, periodYear, periodMonth);
        BigDecimal totalDeductionAmount = deductionList.stream()
                .map(SalaryDeduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pending advances (PENDING status)
        List<SalaryAdvance> pendingAdvances = salaryAdvanceRepository.findByEmployeeIdAndStatus(employeeId, "PENDING");
        BigDecimal advancesRecovered = pendingAdvances.stream()
                .map(a -> a.getAmount().subtract(a.getRecoveredAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gross = base + overtime + bonus (bonus=0 for now, can be added via deductions)
        BigDecimal grossSalary = baseEarned.add(overtimePay).setScale(2, RoundingMode.HALF_UP);

        // Total deductions = salary deductions + advance recovery
        BigDecimal totalDeductions = totalDeductionAmount.add(advancesRecovered).setScale(2, RoundingMode.HALF_UP);

        // Net payable
        BigDecimal netPayable = grossSalary.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
        if (netPayable.compareTo(BigDecimal.ZERO) < 0) {
            netPayable = BigDecimal.ZERO;
        }

        // Amount already paid in this period
        List<SalaryPayment> payments = salaryPaymentRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(employeeId, periodYear, periodMonth);
        BigDecimal paidAmount = payments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balanceRemaining = netPayable.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);

        // Ledger outstanding balance (credit - debit)
        List<EmployeeLedgerEntry> ledgerEntries = employeeLedgerEntryRepository.findByEmployeeIdOrderByTransactionDateDescCreatedAtDesc(employeeId);
        BigDecimal ledgerBalance = BigDecimal.ZERO;
        for (EmployeeLedgerEntry entry : ledgerEntries) {
            ledgerBalance = ledgerBalance.add(entry.getCreditAmount()).subtract(entry.getDebitAmount());
        }

        PayrollCalculationResult result = new PayrollCalculationResult();
        result.setEmployeeId(employee.getId());
        result.setEmployeeCode(employee.getEmployeeCode());
        result.setFullName(employee.getFullName());
        result.setDesignation(employee.getDesignation());
        result.setWageType(employee.getWageType());
        result.setRateAmount(employee.getRateAmount());
        result.setPeriodYear(periodYear);
        result.setPeriodMonth(periodMonth);
        result.setPresentDays(presentDays);
        result.setHalfDays(halfDays);
        result.setAbsentDays(absentDays);
        result.setPayableDays(payableDays);
        result.setOvertimeHours(overtimeHoursTotal);
        result.setBaseEarned(baseEarned);
        result.setOvertimePay(overtimePay);
        result.setBonusAllowance(BigDecimal.ZERO);
        result.setGrossSalary(grossSalary);
        result.setAdvancesRecovered(advancesRecovered);
        result.setDeductions(totalDeductionAmount);
        result.setTotalDeductions(totalDeductions);
        result.setNetPayable(netPayable);
        result.setPaidAmount(paidAmount);
        result.setBalanceRemaining(balanceRemaining);
        result.setLedgerOutstandingBalance(ledgerBalance);

        return result;
    }

    // ===== SALARY ADVANCES =====

    @Transactional
    public SalaryAdvance createAdvance(CreateSalaryAdvanceRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());

        SalaryAdvance advance = new SalaryAdvance();
        advance.setEmployeeId(employee.getId());
        advance.setAdvanceDate(request.getAdvanceDate() != null ? request.getAdvanceDate() : LocalDate.now());
        advance.setAmount(request.getAmount());
        advance.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        advance.setReferenceNumber(request.getReferenceNumber());
        advance.setNotes(request.getNotes());
        advance.setStatus("PENDING");
        advance.setRecoveredAmount(BigDecimal.ZERO);
        advance.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "MANAGER");

        SalaryAdvance saved = salaryAdvanceRepository.save(advance);

        // Post to employee ledger: ADVANCE_PAID is a debit (reduces what company owes)
        BigDecimal currentBalance = computeCurrentLedgerBalance(employee.getId());
        BigDecimal newBalance = currentBalance.subtract(request.getAmount());

        EmployeeLedgerEntry entry = new EmployeeLedgerEntry();
        entry.setEmployeeId(employee.getId());
        entry.setTransactionDate(advance.getAdvanceDate());
        entry.setTransactionType("ADVANCE_PAID");
        entry.setDebitAmount(request.getAmount());
        entry.setCreditAmount(BigDecimal.ZERO);
        entry.setRunningBalance(newBalance);
        entry.setReferenceType("ADVANCE");
        entry.setReferenceId(saved.getId().toString());
        entry.setNotes("Salary advance paid: " + (request.getNotes() != null ? request.getNotes() : ""));
        employeeLedgerEntryRepository.save(entry);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvance> getAdvances(UUID employeeId) {
        if (employeeId != null) {
            return salaryAdvanceRepository.findByEmployeeIdOrderByAdvanceDateDesc(employeeId);
        }
        return salaryAdvanceRepository.findAllByOrderByAdvanceDateDesc();
    }

    // ===== SALARY DEDUCTIONS =====

    @Transactional
    public SalaryDeduction createDeduction(CreateSalaryDeductionRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());

        SalaryDeduction deduction = new SalaryDeduction();
        deduction.setEmployeeId(employee.getId());
        deduction.setPeriodYear(request.getPeriodYear());
        deduction.setPeriodMonth(request.getPeriodMonth());
        deduction.setDeductionDate(request.getDeductionDate() != null ? request.getDeductionDate() : LocalDate.now());
        deduction.setAmount(request.getAmount());
        deduction.setReason(request.getReason());
        deduction.setNotes(request.getNotes());
        deduction.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "MANAGER");

        SalaryDeduction saved = salaryDeductionRepository.save(deduction);

        // Post to employee ledger: DEDUCTION is a debit
        BigDecimal currentBalance = computeCurrentLedgerBalance(employee.getId());
        BigDecimal newBalance = currentBalance.subtract(request.getAmount());

        EmployeeLedgerEntry entry = new EmployeeLedgerEntry();
        entry.setEmployeeId(employee.getId());
        entry.setTransactionDate(deduction.getDeductionDate());
        entry.setTransactionType("DEDUCTION");
        entry.setDebitAmount(request.getAmount());
        entry.setCreditAmount(BigDecimal.ZERO);
        entry.setRunningBalance(newBalance);
        entry.setReferenceType("DEDUCTION");
        entry.setReferenceId(saved.getId().toString());
        entry.setNotes("Deduction: " + request.getReason());
        employeeLedgerEntryRepository.save(entry);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SalaryDeduction> getDeductions(UUID employeeId) {
        if (employeeId != null) {
            return salaryDeductionRepository.findByEmployeeIdOrderByDeductionDateDesc(employeeId);
        }
        return salaryDeductionRepository.findAllByOrderByDeductionDateDesc();
    }

    // ===== SALARY PAYMENTS =====

    @Transactional
    public SalaryPayment recordSalaryPayment(RecordSalaryPaymentRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());

        // Payment integrity guard: Validate that payment does not exceed outstanding balance / net payable
        PayrollCalculationResult calc = calculatePayroll(employee.getId(), request.getPeriodYear(), request.getPeriodMonth());
        BigDecimal maxAllowed = calc.getBalanceRemaining();
        if (maxAllowed != null && maxAllowed.compareTo(BigDecimal.ZERO) > 0 && request.getAmount().compareTo(maxAllowed) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Payment integrity violation: Requested payment (₹%s) exceeds the net payable balance (₹%s) for %d/%d.",
                    request.getAmount(), maxAllowed, request.getPeriodYear(), request.getPeriodMonth()));
        }

        SalaryPayment payment = new SalaryPayment();
        payment.setEmployeeId(employee.getId());
        payment.setPeriodYear(request.getPeriodYear());
        payment.setPeriodMonth(request.getPeriodMonth());
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        payment.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "MANAGER");

        SalaryPayment saved = salaryPaymentRepository.save(payment);

        // Post to employee ledger: SALARY_PAYMENT is a debit (reduces what company owes)
        BigDecimal currentBalance = computeCurrentLedgerBalance(employee.getId());
        BigDecimal newBalance = currentBalance.subtract(request.getAmount());

        EmployeeLedgerEntry entry = new EmployeeLedgerEntry();
        entry.setEmployeeId(employee.getId());
        entry.setTransactionDate(payment.getPaymentDate());
        entry.setTransactionType("SALARY_PAYMENT");
        entry.setDebitAmount(request.getAmount());
        entry.setCreditAmount(BigDecimal.ZERO);
        entry.setRunningBalance(newBalance);
        entry.setReferenceType("PAYMENT");
        entry.setReferenceId(saved.getId().toString());
        entry.setNotes("Salary payment for " + request.getPeriodYear() + "/" + request.getPeriodMonth()
                + (request.getNotes() != null ? " - " + request.getNotes() : ""));
        employeeLedgerEntryRepository.save(entry);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SalaryPayment> getPayments(UUID employeeId, Integer periodYear, Integer periodMonth) {
        if (employeeId != null) {
            return salaryPaymentRepository.findByEmployeeIdOrderByPaymentDateDesc(employeeId);
        }
        if (periodYear != null && periodMonth != null) {
            return salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByPaymentDateDesc(periodYear, periodMonth);
        }
        return salaryPaymentRepository.findAllByOrderByPaymentDateDesc();
    }

    // ===== PAYROLL RECORDS (legacy) =====

    @Transactional(readOnly = true)
    public List<com.rrjaggery.finance.entity.PayrollRecord> getPayrollRecords(Integer year, Integer month) {
        if (year != null && month != null) {
            return payrollRecordRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(year, month);
        }
        return payrollRecordRepository.findAllByOrderByPaymentDateDesc();
    }

    @Transactional
    public com.rrjaggery.finance.entity.PayrollRecord createPayrollRecord(CreatePayrollRequest request) {
        Employee employee = getEmployee(request.getEmployeeId());

        // Accrue salary in employee ledger: SALARY_ACCRUED is a credit
        BigDecimal grossSalary = (request.getBaseAmount() != null ? request.getBaseAmount() : BigDecimal.ZERO)
                .add(request.getBonusAllowance() != null ? request.getBonusAllowance() : BigDecimal.ZERO);

        com.rrjaggery.finance.entity.PayrollRecord record = new com.rrjaggery.finance.entity.PayrollRecord();
        record.setEmployeeId(employee.getId());
        record.setPeriodMonth(request.getPeriodMonth());
        record.setPeriodYear(request.getPeriodYear());
        record.setDaysWorked(request.getDaysWorked() != null ? request.getDaysWorked() : BigDecimal.ZERO);
        record.setOvertimeHours(request.getOvertimeHours() != null ? request.getOvertimeHours() : BigDecimal.ZERO);
        record.setBaseAmount(request.getBaseAmount() != null ? request.getBaseAmount() : BigDecimal.ZERO);
        record.setBonusAllowance(request.getBonusAllowance() != null ? request.getBonusAllowance() : BigDecimal.ZERO);
        record.setDeductions(request.getDeductions() != null ? request.getDeductions() : BigDecimal.ZERO);
        record.setNetAmountPaid(request.getNetAmountPaid());
        record.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        record.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        record.setReferenceNotes(request.getReferenceNotes());

        com.rrjaggery.finance.entity.PayrollRecord saved = payrollRecordRepository.save(record);

        // Post salary accrual to ledger
        BigDecimal currentBalance = computeCurrentLedgerBalance(employee.getId());
        BigDecimal newBalance = currentBalance.add(grossSalary);

        EmployeeLedgerEntry accrualEntry = new EmployeeLedgerEntry();
        accrualEntry.setEmployeeId(employee.getId());
        accrualEntry.setTransactionDate(LocalDate.now());
        accrualEntry.setTransactionType("SALARY_ACCRUED");
        accrualEntry.setDebitAmount(BigDecimal.ZERO);
        accrualEntry.setCreditAmount(grossSalary);
        accrualEntry.setRunningBalance(newBalance);
        accrualEntry.setReferenceType("PAYROLL");
        accrualEntry.setReferenceId(saved.getId().toString());
        accrualEntry.setNotes("Salary accrual for " + request.getPeriodYear() + "/" + request.getPeriodMonth());
        employeeLedgerEntryRepository.save(accrualEntry);

        return saved;
    }

    // ===== EMPLOYEE LEDGER =====

    @Transactional(readOnly = true)
    public List<EmployeeLedgerEntry> getEmployeeLedger(UUID employeeId) {
        return employeeLedgerEntryRepository.findByEmployeeIdOrderByTransactionDateDescCreatedAtDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLedgerEntry> getAllLedgerEntries() {
        return employeeLedgerEntryRepository.findAllByOrderByTransactionDateDescCreatedAtDesc();
    }

    // ===== PRIVATE HELPERS =====

    private BigDecimal computeCurrentLedgerBalance(UUID employeeId) {
        List<EmployeeLedgerEntry> entries = employeeLedgerEntryRepository.findByEmployeeIdOrderByTransactionDateDescCreatedAtDesc(employeeId);
        if (entries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return entries.get(0).getRunningBalance();
    }
}
