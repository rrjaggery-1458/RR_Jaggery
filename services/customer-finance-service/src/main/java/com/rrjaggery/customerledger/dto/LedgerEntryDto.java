package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.CustomerLedgerEntry;
import com.rrjaggery.customerledger.entity.LedgerTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LedgerEntryDto {

    private UUID id;
    private UUID customerId;
    private Instant transactionDate;
    private LedgerTransactionType transactionType;
    private String referenceType;
    private String referenceId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balanceAfter;
    private Instant dueDate;
    private boolean overdue;
    private String description;
    private String createdBy;
    private Instant createdAt;

    public LedgerEntryDto() {}

    public static LedgerEntryDto fromEntity(CustomerLedgerEntry l) {
        LedgerEntryDto dto = new LedgerEntryDto();
        dto.setId(l.getId());
        dto.setCustomerId(l.getCustomer().getId());
        dto.setTransactionDate(l.getTransactionDate());
        dto.setTransactionType(l.getTransactionType());
        dto.setReferenceType(l.getReferenceType());
        dto.setReferenceId(l.getReferenceId());
        dto.setDebitAmount(l.getDebitAmount());
        dto.setCreditAmount(l.getCreditAmount());
        dto.setBalanceAfter(l.getBalanceAfter());
        dto.setDueDate(l.getDueDate());
        dto.setOverdue(l.getDueDate() != null && l.getDueDate().isBefore(Instant.now()) && l.getBalanceAfter().compareTo(BigDecimal.ZERO) > 0);
        dto.setDescription(l.getDescription());
        dto.setCreatedBy(l.getCreatedBy());
        dto.setCreatedAt(l.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public Instant getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Instant transactionDate) { this.transactionDate = transactionDate; }

    public LedgerTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(LedgerTransactionType transactionType) { this.transactionType = transactionType; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
