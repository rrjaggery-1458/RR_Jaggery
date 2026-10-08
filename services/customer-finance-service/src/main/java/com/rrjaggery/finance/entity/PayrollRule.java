package com.rrjaggery.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_rules", schema = "finance_schema")
public class PayrollRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName = "Standard Factory Payroll Standards";

    @Column(name = "standard_working_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal standardWorkingDays = new BigDecimal("26.00");

    @Column(name = "standard_hours_per_day", nullable = false, precision = 5, scale = 2)
    private BigDecimal standardHoursPerDay = new BigDecimal("8.00");

    @Column(name = "overtime_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeMultiplier = new BigDecimal("1.50");

    @Column(name = "half_day_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal halfDayFactor = new BigDecimal("0.50");

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public BigDecimal getStandardWorkingDays() { return standardWorkingDays; }
    public void setStandardWorkingDays(BigDecimal standardWorkingDays) { this.standardWorkingDays = standardWorkingDays; }

    public BigDecimal getStandardHoursPerDay() { return standardHoursPerDay; }
    public void setStandardHoursPerDay(BigDecimal standardHoursPerDay) { this.standardHoursPerDay = standardHoursPerDay; }

    public BigDecimal getOvertimeMultiplier() { return overtimeMultiplier; }
    public void setOvertimeMultiplier(BigDecimal overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }

    public BigDecimal getHalfDayFactor() { return halfDayFactor; }
    public void setHalfDayFactor(BigDecimal halfDayFactor) { this.halfDayFactor = halfDayFactor; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
