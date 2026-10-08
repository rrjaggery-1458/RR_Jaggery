package com.rrjaggery.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class BalanceTransactionDto {
    private String id;
    private Instant date;
    private String type; // INITIAL_INVESTMENT, ADDITIONAL_INVESTMENT, OPERATING_EXPENSE, WHOLESALE_ORDER_INCOME, WEBSITE_RETAIL_INCOME, MANUAL_RETAIL_INCOME
    private String category;
    private String description;
    private String reference;
    private String entityType;
    private String entityId;
    private BigDecimal amount; // positive for income/investment, negative for expense
    private BigDecimal balanceAfter;

    public BalanceTransactionDto() {}

    public BalanceTransactionDto(String id, Instant date, String type, String category,
                                 String description, String reference, String entityType,
                                 String entityId, BigDecimal amount, BigDecimal balanceAfter) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.category = category;
        this.description = description;
        this.reference = reference;
        this.entityType = entityType;
        this.entityId = entityId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
}
