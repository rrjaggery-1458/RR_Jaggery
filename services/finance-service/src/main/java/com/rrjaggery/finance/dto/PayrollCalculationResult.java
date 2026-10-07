package com.rrjaggery.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PayrollCalculationResult {

    private UUID employeeId;
    private String employeeCode;
    private String fullName;
    private String designation;
    private String wageType;
    private BigDecimal rateAmount;
    private int periodYear;
    private int periodMonth;

    private BigDecimal presentDays = BigDecimal.ZERO;
    private BigDecimal halfDays = BigDecimal.ZERO;
    private BigDecimal absentDays = BigDecimal.ZERO;
    private BigDecimal payableDays = BigDecimal.ZERO;
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    private BigDecimal baseEarned = BigDecimal.ZERO;
    private BigDecimal overtimePay = BigDecimal.ZERO;
    private BigDecimal bonusAllowance = BigDecimal.ZERO;
    private BigDecimal grossSalary = BigDecimal.ZERO;

    private BigDecimal advancesRecovered = BigDecimal.ZERO;
    private BigDecimal deductions = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    private BigDecimal netPayable = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal balanceRemaining = BigDecimal.ZERO;
    private BigDecimal ledgerOutstandingBalance = BigDecimal.ZERO;

    public PayrollCalculationResult() {}

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getWageType() { return wageType; }
    public void setWageType(String wageType) { this.wageType = wageType; }

    public BigDecimal getRateAmount() { return rateAmount; }
    public void setRateAmount(BigDecimal rateAmount) { this.rateAmount = rateAmount; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public BigDecimal getPresentDays() { return presentDays; }
    public void setPresentDays(BigDecimal presentDays) { this.presentDays = presentDays; }

    public BigDecimal getHalfDays() { return halfDays; }
    public void setHalfDays(BigDecimal halfDays) { this.halfDays = halfDays; }

    public BigDecimal getAbsentDays() { return absentDays; }
    public void setAbsentDays(BigDecimal absentDays) { this.absentDays = absentDays; }

    public BigDecimal getPayableDays() { return payableDays; }
    public void setPayableDays(BigDecimal payableDays) { this.payableDays = payableDays; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getBaseEarned() { return baseEarned; }
    public void setBaseEarned(BigDecimal baseEarned) { this.baseEarned = baseEarned; }

    public BigDecimal getOvertimePay() { return overtimePay; }
    public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }

    public BigDecimal getBonusAllowance() { return bonusAllowance; }
    public void setBonusAllowance(BigDecimal bonusAllowance) { this.bonusAllowance = bonusAllowance; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getAdvancesRecovered() { return advancesRecovered; }
    public void setAdvancesRecovered(BigDecimal advancesRecovered) { this.advancesRecovered = advancesRecovered; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNetPayable() { return netPayable; }
    public void setNetPayable(BigDecimal netPayable) { this.netPayable = netPayable; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalanceRemaining() { return balanceRemaining; }
    public void setBalanceRemaining(BigDecimal balanceRemaining) { this.balanceRemaining = balanceRemaining; }

    public BigDecimal getLedgerOutstandingBalance() { return ledgerOutstandingBalance; }
    public void setLedgerOutstandingBalance(BigDecimal ledgerOutstandingBalance) { this.ledgerOutstandingBalance = ledgerOutstandingBalance; }
}
