package com.rrjaggery.customerledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CustomerStatementDto {

    private UUID customerId;
    private String customerName;
    private String businessName;
    private String phone;
    private String email;
    private String gstin;
    private String customerType;
    private BigDecimal creditLimit;
    private int creditDays;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal currentOutstanding;
    private BigDecimal totalOverdue;
    private Instant generatedAt;
    private List<LedgerEntryDto> entries;

    public CustomerStatementDto() {}

    // Getters and Setters
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public int getCreditDays() { return creditDays; }
    public void setCreditDays(int creditDays) { this.creditDays = creditDays; }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public BigDecimal getCurrentOutstanding() { return currentOutstanding; }
    public void setCurrentOutstanding(BigDecimal currentOutstanding) { this.currentOutstanding = currentOutstanding; }

    public BigDecimal getTotalOverdue() { return totalOverdue; }
    public void setTotalOverdue(BigDecimal totalOverdue) { this.totalOverdue = totalOverdue; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public List<LedgerEntryDto> getEntries() { return entries; }
    public void setEntries(List<LedgerEntryDto> entries) { this.entries = entries; }
}
