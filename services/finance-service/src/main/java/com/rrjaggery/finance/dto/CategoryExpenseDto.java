package com.rrjaggery.finance.dto;

import java.math.BigDecimal;

public class CategoryExpenseDto {
    private String category;
    private BigDecimal amount;
    private double percentage;

    public CategoryExpenseDto() {}

    public CategoryExpenseDto(String category, BigDecimal amount, double percentage) {
        this.category = category;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}
