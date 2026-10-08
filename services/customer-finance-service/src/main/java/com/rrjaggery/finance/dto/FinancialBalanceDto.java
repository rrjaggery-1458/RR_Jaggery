package com.rrjaggery.finance.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FinancialBalanceDto {

    // All-time Running Balance Metrics
    private BigDecimal initialInvestment = BigDecimal.ZERO;
    private BigDecimal additionalInvestments = BigDecimal.ZERO;
    private BigDecimal totalInvestments = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal totalSalaries = BigDecimal.ZERO;
    private BigDecimal totalOperatingCosts = BigDecimal.ZERO;
    private BigDecimal wholesaleOrderIncome = BigDecimal.ZERO;
    private BigDecimal websiteRetailOrderIncome = BigDecimal.ZERO;
    private BigDecimal manualRetailOrderIncome = BigDecimal.ZERO;
    private BigDecimal totalOrderIncome = BigDecimal.ZERO;
    private BigDecimal currentOperationalBalance = BigDecimal.ZERO;

    // Period-specific Reporting (Monthly / Yearly)
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal periodAdditionalInvestments = BigDecimal.ZERO;
    private BigDecimal periodExpenses = BigDecimal.ZERO;
    private BigDecimal periodSalaries = BigDecimal.ZERO;
    private BigDecimal periodOperatingCosts = BigDecimal.ZERO;
    private BigDecimal periodWholesaleIncome = BigDecimal.ZERO;
    private BigDecimal periodWebsiteRetailIncome = BigDecimal.ZERO;
    private BigDecimal periodManualRetailIncome = BigDecimal.ZERO;
    private BigDecimal periodTotalOrderIncome = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;

    // Stock Valuation (Kept strictly separate from Operational Balance)
    private BigDecimal finishedGoodsStockValue = BigDecimal.ZERO;
    private BigDecimal rawMaterialStockValue = BigDecimal.ZERO;
    private String rawMaterialValuationStatus = "VALUATION_UNAVAILABLE";
    private BigDecimal totalStockValue = BigDecimal.ZERO;
    private BigDecimal totalBusinessPosition = BigDecimal.ZERO;

    private List<StockValuationItemDto> finishedGoodsStockDetails = new ArrayList<>();
    private List<StockValuationItemDto> rawMaterialStockDetails = new ArrayList<>();

    // Transaction Traceability
    private List<BalanceTransactionDto> recentTransactions = new ArrayList<>();

    public FinancialBalanceDto() {}

    // Getters and Setters
    public BigDecimal getInitialInvestment() { return initialInvestment; }
    public void setInitialInvestment(BigDecimal initialInvestment) { this.initialInvestment = initialInvestment; }

    public BigDecimal getAdditionalInvestments() { return additionalInvestments; }
    public void setAdditionalInvestments(BigDecimal additionalInvestments) { this.additionalInvestments = additionalInvestments; }

    public BigDecimal getTotalInvestments() { return totalInvestments; }
    public void setTotalInvestments(BigDecimal totalInvestments) { this.totalInvestments = totalInvestments; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getTotalSalaries() { return totalSalaries; }
    public void setTotalSalaries(BigDecimal totalSalaries) { this.totalSalaries = totalSalaries; }

    public BigDecimal getTotalOperatingCosts() { return totalOperatingCosts; }
    public void setTotalOperatingCosts(BigDecimal totalOperatingCosts) { this.totalOperatingCosts = totalOperatingCosts; }

    public BigDecimal getWholesaleOrderIncome() { return wholesaleOrderIncome; }
    public void setWholesaleOrderIncome(BigDecimal wholesaleOrderIncome) { this.wholesaleOrderIncome = wholesaleOrderIncome; }

    public BigDecimal getWebsiteRetailOrderIncome() { return websiteRetailOrderIncome; }
    public void setWebsiteRetailOrderIncome(BigDecimal websiteRetailOrderIncome) { this.websiteRetailOrderIncome = websiteRetailOrderIncome; }

    public BigDecimal getManualRetailOrderIncome() { return manualRetailOrderIncome; }
    public void setManualRetailOrderIncome(BigDecimal manualRetailOrderIncome) { this.manualRetailOrderIncome = manualRetailOrderIncome; }

    public BigDecimal getTotalOrderIncome() { return totalOrderIncome; }
    public void setTotalOrderIncome(BigDecimal totalOrderIncome) { this.totalOrderIncome = totalOrderIncome; }

    public BigDecimal getCurrentOperationalBalance() { return currentOperationalBalance; }
    public void setCurrentOperationalBalance(BigDecimal currentOperationalBalance) { this.currentOperationalBalance = currentOperationalBalance; }

    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }

    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getPeriodAdditionalInvestments() { return periodAdditionalInvestments; }
    public void setPeriodAdditionalInvestments(BigDecimal periodAdditionalInvestments) { this.periodAdditionalInvestments = periodAdditionalInvestments; }

    public BigDecimal getPeriodExpenses() { return periodExpenses; }
    public void setPeriodExpenses(BigDecimal periodExpenses) { this.periodExpenses = periodExpenses; }

    public BigDecimal getPeriodSalaries() { return periodSalaries; }
    public void setPeriodSalaries(BigDecimal periodSalaries) { this.periodSalaries = periodSalaries; }

    public BigDecimal getPeriodOperatingCosts() { return periodOperatingCosts; }
    public void setPeriodOperatingCosts(BigDecimal periodOperatingCosts) { this.periodOperatingCosts = periodOperatingCosts; }

    public BigDecimal getPeriodWholesaleIncome() { return periodWholesaleIncome; }
    public void setPeriodWholesaleIncome(BigDecimal periodWholesaleIncome) { this.periodWholesaleIncome = periodWholesaleIncome; }

    public BigDecimal getPeriodWebsiteRetailIncome() { return periodWebsiteRetailIncome; }
    public void setPeriodWebsiteRetailIncome(BigDecimal periodWebsiteRetailIncome) { this.periodWebsiteRetailIncome = periodWebsiteRetailIncome; }

    public BigDecimal getPeriodManualRetailIncome() { return periodManualRetailIncome; }
    public void setPeriodManualRetailIncome(BigDecimal periodManualRetailIncome) { this.periodManualRetailIncome = periodManualRetailIncome; }

    public BigDecimal getPeriodTotalOrderIncome() { return periodTotalOrderIncome; }
    public void setPeriodTotalOrderIncome(BigDecimal periodTotalOrderIncome) { this.periodTotalOrderIncome = periodTotalOrderIncome; }

    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }

    public BigDecimal getFinishedGoodsStockValue() { return finishedGoodsStockValue; }
    public void setFinishedGoodsStockValue(BigDecimal finishedGoodsStockValue) { this.finishedGoodsStockValue = finishedGoodsStockValue; }

    public BigDecimal getRawMaterialStockValue() { return rawMaterialStockValue; }
    public void setRawMaterialStockValue(BigDecimal rawMaterialStockValue) { this.rawMaterialStockValue = rawMaterialStockValue; }

    public String getRawMaterialValuationStatus() { return rawMaterialValuationStatus; }
    public void setRawMaterialValuationStatus(String rawMaterialValuationStatus) { this.rawMaterialValuationStatus = rawMaterialValuationStatus; }

    public BigDecimal getTotalStockValue() { return totalStockValue; }
    public void setTotalStockValue(BigDecimal totalStockValue) { this.totalStockValue = totalStockValue; }

    public BigDecimal getTotalBusinessPosition() { return totalBusinessPosition; }
    public void setTotalBusinessPosition(BigDecimal totalBusinessPosition) { this.totalBusinessPosition = totalBusinessPosition; }

    public List<StockValuationItemDto> getFinishedGoodsStockDetails() { return finishedGoodsStockDetails; }
    public void setFinishedGoodsStockDetails(List<StockValuationItemDto> finishedGoodsStockDetails) { this.finishedGoodsStockDetails = finishedGoodsStockDetails; }

    public List<StockValuationItemDto> getRawMaterialStockDetails() { return rawMaterialStockDetails; }
    public void setRawMaterialStockDetails(List<StockValuationItemDto> rawMaterialStockDetails) { this.rawMaterialStockDetails = rawMaterialStockDetails; }

    public List<BalanceTransactionDto> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<BalanceTransactionDto> recentTransactions) { this.recentTransactions = recentTransactions; }
}
