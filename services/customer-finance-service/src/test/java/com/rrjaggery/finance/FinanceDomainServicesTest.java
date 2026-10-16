package com.rrjaggery.finance;

import com.rrjaggery.finance.dto.CategoryExpenseDto;
import com.rrjaggery.finance.dto.CreateEmployeeRequest;
import com.rrjaggery.finance.dto.CreateExpenseRequest;
import com.rrjaggery.finance.dto.CreatePayrollRequest;
import com.rrjaggery.finance.dto.RecordAttendanceRequest;
import com.rrjaggery.finance.dto.RecordSalaryPaymentRequest;
import com.rrjaggery.finance.entity.Attendance;
import com.rrjaggery.finance.entity.AttendanceStatus;
import com.rrjaggery.finance.entity.Employee;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.entity.PayrollRecord;
import com.rrjaggery.finance.entity.SalaryPayment;
import com.rrjaggery.finance.service.ExpenseService;
import com.rrjaggery.finance.service.PayrollService;
import com.rrjaggery.customerledger.service.OfflineInventoryStockClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinanceDomainServicesTest {

    @MockBean
    private OfflineInventoryStockClient offlineInventoryStockClient;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private PayrollService payrollService;

    @Test
    @DisplayName("Should create and aggregate expenses by category")
    void testExpenseCreationAndBreakdown() {
        CreateExpenseRequest req1 = new CreateExpenseRequest();
        req1.setExpenseDate(LocalDate.now());
        req1.setCategory("Diesel / Fuel");
        req1.setAmount(new BigDecimal("3500.00"));
        req1.setPaymentMethod("UPI");
        req1.setDescription("Diesel for crusher");
        Expense exp1 = expenseService.createExpense(req1);
        assertNotNull(exp1.getId());

        CreateExpenseRequest req2 = new CreateExpenseRequest();
        req2.setExpenseDate(LocalDate.now());
        req2.setCategory("Packaging");
        req2.setAmount(new BigDecimal("1500.00"));
        req2.setPaymentMethod("CASH");
        req2.setDescription("Gunny bags");
        Expense exp2 = expenseService.createExpense(req2);
        assertNotNull(exp2.getId());

        List<CategoryExpenseDto> breakdown = expenseService.getExpenseBreakdown(LocalDate.now().getYear(), LocalDate.now().getMonthValue());

        assertFalse(breakdown.isEmpty());
        assertTrue(breakdown.stream().anyMatch(b -> b.getCategory().equals("Diesel / Fuel") && b.getAmount().compareTo(new BigDecimal("3500.00")) == 0));
        assertTrue(breakdown.stream().anyMatch(b -> b.getCategory().equals("Packaging") && b.getAmount().compareTo(new BigDecimal("1500.00")) == 0));
    }

    @Test
    @DisplayName("Should manage employees, attendance with overtime, and wage payment records")
    void testEmployeeAttendanceAndPayroll() {
        CreateEmployeeRequest empReq = new CreateEmployeeRequest();
        empReq.setFullName("Ramesh Kumar");
        empReq.setDesignation("Boiler Operator");
        empReq.setPhone("9876543210");
        empReq.setWageType("DAILY");
        empReq.setRateAmount(new BigDecimal("600.00"));
        Employee emp = payrollService.createEmployee(empReq);
        assertNotNull(emp.getId());
        assertTrue(emp.getEmployeeCode().startsWith("EMP-"));

        RecordAttendanceRequest attReq = new RecordAttendanceRequest();
        attReq.setEmployeeId(emp.getId());
        attReq.setAttendanceDate(LocalDate.now());
        attReq.setStatus(AttendanceStatus.PRESENT);
        attReq.setOvertimeHours(new BigDecimal("2.5"));
        attReq.setNotes("Late shift processing");
        Attendance att = payrollService.recordAttendance(attReq);
        assertNotNull(att.getId());
        assertEquals(new BigDecimal("2.5"), att.getOvertimeHours());

        CreatePayrollRequest payReq = new CreatePayrollRequest();
        payReq.setEmployeeId(emp.getId());
        payReq.setPeriodMonth(LocalDate.now().getMonthValue());
        payReq.setPeriodYear(LocalDate.now().getYear());
        payReq.setDaysWorked(new BigDecimal("26"));
        payReq.setOvertimeHours(new BigDecimal("10"));
        payReq.setBaseAmount(new BigDecimal("15600.00"));
        payReq.setBonusAllowance(new BigDecimal("1000.00"));
        payReq.setDeductions(new BigDecimal("500.00"));
        payReq.setNetAmountPaid(new BigDecimal("16100.00"));
        payReq.setPaymentMethod("BANK_TRANSFER");
        PayrollRecord pay = payrollService.createPayrollRecord(payReq);
        assertNotNull(pay.getId());
        assertEquals(new BigDecimal("16100.00"), pay.getNetAmountPaid());
    }

    @Test
    @DisplayName("Attendance updates one employee-day record and rejects future dates")
    void attendanceIsUpdatedInPlaceAndFutureDatesAreRejected() {
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setFullName("Attendance Test Worker");
        employeeRequest.setDesignation("Operator");
        employeeRequest.setWageType("DAILY");
        employeeRequest.setRateAmount(new BigDecimal("600.00"));
        Employee employee = payrollService.createEmployee(employeeRequest);

        LocalDate attendanceDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
        RecordAttendanceRequest request = new RecordAttendanceRequest();
        request.setEmployeeId(employee.getId());
        request.setAttendanceDate(attendanceDate);
        request.setStatus(AttendanceStatus.ABSENT);
        Attendance created = payrollService.recordAttendance(request);

        request.setStatus(AttendanceStatus.PRESENT);
        Attendance updated = payrollService.recordAttendance(request);

        assertEquals(created.getId(), updated.getId());
        assertEquals(AttendanceStatus.PRESENT, updated.getStatus());
        assertEquals(1, payrollService.getEmployeeAttendance(employee.getId(), attendanceDate, attendanceDate).size());

        request.setAttendanceDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> payrollService.recordAttendance(request));
    }

    @Test
    @DisplayName("Salary payments are limited to the server-calculated remaining balance")
    void salaryPaymentCannotExceedOrRepeatTheRemainingBalance() {
        CreateEmployeeRequest employeeRequest = new CreateEmployeeRequest();
        employeeRequest.setFullName("Payroll Test Worker");
        employeeRequest.setDesignation("Operator");
        employeeRequest.setWageType("DAILY");
        employeeRequest.setRateAmount(new BigDecimal("600.00"));
        Employee employee = payrollService.createEmployee(employeeRequest);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        RecordAttendanceRequest attendanceRequest = new RecordAttendanceRequest();
        attendanceRequest.setEmployeeId(employee.getId());
        attendanceRequest.setAttendanceDate(today);
        attendanceRequest.setStatus(AttendanceStatus.PRESENT);
        payrollService.recordAttendance(attendanceRequest);

        var calculation = payrollService.calculatePayroll(employee.getId(), today.getYear(), today.getMonthValue());
        BigDecimal remainingBalance = calculation.getBalanceRemaining();
        assertTrue(remainingBalance.compareTo(BigDecimal.ZERO) > 0);

        RecordSalaryPaymentRequest request = new RecordSalaryPaymentRequest();
        request.setEmployeeId(employee.getId());
        request.setPeriodYear(today.getYear());
        request.setPeriodMonth(today.getMonthValue());
        request.setPaymentDate(today);
        request.setAmount(remainingBalance.add(new BigDecimal("0.01")));
        assertThrows(IllegalArgumentException.class, () -> payrollService.recordSalaryPayment(request));

        request.setAmount(remainingBalance);
        SalaryPayment payment = payrollService.recordSalaryPayment(request);
        assertEquals(remainingBalance, payment.getAmount());

        request.setAmount(new BigDecimal("0.01"));
        assertThrows(IllegalArgumentException.class, () -> payrollService.recordSalaryPayment(request));
    }

    @Test
    @DisplayName("Monthly and weekly payroll exclude Sunday attendance and overtime")
    void monthlyAndWeeklyPayrollDoNotPayForSundays() {
        LocalDate sunday = LocalDate.of(2026, 6, 7);
        LocalDate monday = sunday.plusDays(1);

        CreateEmployeeRequest monthlyRequest = new CreateEmployeeRequest();
        monthlyRequest.setFullName("Monthly Payroll Test Worker");
        monthlyRequest.setDesignation("Operator");
        monthlyRequest.setWageType("MONTHLY");
        monthlyRequest.setRateAmount(new BigDecimal("2600.00"));
        Employee monthlyEmployee = payrollService.createEmployee(monthlyRequest);

        RecordAttendanceRequest monthlySunday = new RecordAttendanceRequest();
        monthlySunday.setEmployeeId(monthlyEmployee.getId());
        monthlySunday.setAttendanceDate(sunday);
        monthlySunday.setStatus(AttendanceStatus.PRESENT);
        monthlySunday.setOvertimeHours(new BigDecimal("8.00"));
        payrollService.recordAttendance(monthlySunday);

        RecordAttendanceRequest monthlyMonday = new RecordAttendanceRequest();
        monthlyMonday.setEmployeeId(monthlyEmployee.getId());
        monthlyMonday.setAttendanceDate(monday);
        monthlyMonday.setStatus(AttendanceStatus.PRESENT);
        payrollService.recordAttendance(monthlyMonday);

        var monthlyCalculation = payrollService.calculatePayroll(monthlyEmployee.getId(), 2026, 6);
        assertEquals(0, new BigDecimal("1").compareTo(monthlyCalculation.getPresentDays()));
        assertEquals(0, new BigDecimal("1").compareTo(monthlyCalculation.getPayableDays()));
        assertEquals(0, new BigDecimal("100.00").compareTo(monthlyCalculation.getBaseEarned()));
        assertEquals(0, BigDecimal.ZERO.compareTo(monthlyCalculation.getOvertimeHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(monthlyCalculation.getOvertimePay()));

        CreateEmployeeRequest weeklyRequest = new CreateEmployeeRequest();
        weeklyRequest.setFullName("Weekly Payroll Test Worker");
        weeklyRequest.setDesignation("Operator");
        weeklyRequest.setWageType("WEEKLY");
        weeklyRequest.setRateAmount(new BigDecimal("600.00"));
        Employee weeklyEmployee = payrollService.createEmployee(weeklyRequest);

        RecordAttendanceRequest weeklySunday = new RecordAttendanceRequest();
        weeklySunday.setEmployeeId(weeklyEmployee.getId());
        weeklySunday.setAttendanceDate(sunday);
        weeklySunday.setStatus(AttendanceStatus.PRESENT);
        weeklySunday.setOvertimeHours(new BigDecimal("8.00"));
        payrollService.recordAttendance(weeklySunday);

        RecordAttendanceRequest weeklyMonday = new RecordAttendanceRequest();
        weeklyMonday.setEmployeeId(weeklyEmployee.getId());
        weeklyMonday.setAttendanceDate(monday);
        weeklyMonday.setStatus(AttendanceStatus.PRESENT);
        payrollService.recordAttendance(weeklyMonday);

        var weeklyCalculation = payrollService.calculatePayroll(weeklyEmployee.getId(), 2026, 6);
        assertEquals(0, new BigDecimal("1").compareTo(weeklyCalculation.getPresentDays()));
        assertEquals(0, new BigDecimal("100.00").compareTo(weeklyCalculation.getBaseEarned()));
        assertEquals(0, BigDecimal.ZERO.compareTo(weeklyCalculation.getOvertimeHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(weeklyCalculation.getOvertimePay()));
    }
}
