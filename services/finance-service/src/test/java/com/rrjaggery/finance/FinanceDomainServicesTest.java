package com.rrjaggery.finance;

import com.rrjaggery.finance.dto.CategoryExpenseDto;
import com.rrjaggery.finance.dto.CreateEmployeeRequest;
import com.rrjaggery.finance.dto.CreateExpenseRequest;
import com.rrjaggery.finance.dto.CreatePayrollRequest;
import com.rrjaggery.finance.dto.RecordAttendanceRequest;
import com.rrjaggery.finance.entity.Attendance;
import com.rrjaggery.finance.entity.AttendanceStatus;
import com.rrjaggery.finance.entity.Employee;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.entity.PayrollRecord;
import com.rrjaggery.finance.service.ExpenseService;
import com.rrjaggery.finance.service.PayrollService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinanceDomainServicesTest {

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
}
