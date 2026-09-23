package com.rrjaggery.procurement.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supplier_ledger_entries", schema = "procurement_schema",
        indexes = {
                @Index(name = "idx_supplier_ledger_supplier", columnList = "supplier_id"),
                @Index(name = "idx_supplier_ledger_reference", columnList = "reference_id")
        })
public class SupplierLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "running_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal runningBalance = BigDecimal.ZERO;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SupplierLedgerEntry() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(LedgerEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount == null ? BigDecimal.ZERO : debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount == null ? BigDecimal.ZERO : debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount == null ? BigDecimal.ZERO : creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount == null ? BigDecimal.ZERO : creditAmount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance == null ? BigDecimal.ZERO : runningBalance;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
