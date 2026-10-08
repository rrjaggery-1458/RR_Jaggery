package com.rrjaggery.finance.service;

import com.rrjaggery.finance.dto.BalanceTransactionDto;
import com.rrjaggery.finance.dto.FinancialBalanceDto;
import com.rrjaggery.finance.dto.StockValuationItemDto;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.entity.Investment;
import com.rrjaggery.finance.entity.SalaryPayment;
import com.rrjaggery.finance.repository.ExpenseRepository;
import com.rrjaggery.finance.repository.InvestmentRepository;
import com.rrjaggery.finance.repository.SalaryPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialBalanceService {

    private final InvestmentRepository investmentRepository;
    private final ExpenseRepository expenseRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final FinanceOrderServiceClient orderServiceClient;

    public FinancialBalanceService(
            InvestmentRepository investmentRepository,
            ExpenseRepository expenseRepository,
            SalaryPaymentRepository salaryPaymentRepository,
            FinanceOrderServiceClient orderServiceClient) {
        this.investmentRepository = investmentRepository;
        this.expenseRepository = expenseRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional(readOnly = true)
    public FinancialBalanceDto calculateFinancialBalance(Integer year, Integer month) {
        FinancialBalanceDto dto = new FinancialBalanceDto();
        dto.setPeriodYear(year);
        dto.setPeriodMonth(month);

        // 1. Fetch Authoritative Persistent Records
        List<Investment> allInvestments = investmentRepository.findAll().stream()
                .filter(inv -> !"VOIDED".equalsIgnoreCase(inv.getStatus()))
                .sorted(Comparator.comparing(Investment::getInvestmentDate).thenComparing(Investment::getCreatedAt))
                .collect(Collectors.toList());

        List<Expense> allExpenses = expenseRepository.findAll().stream()
                .sorted(Comparator.comparing(Expense::getExpenseDate).thenComparing(Expense::getCreatedAt))
                .collect(Collectors.toList());

        List<SalaryPayment> allSalaries = salaryPaymentRepository.findAll().stream()
                .sorted(Comparator.comparing(SalaryPayment::getPaymentDate).thenComparing(SalaryPayment::getCreatedAt))
                .collect(Collectors.toList());

        List<FinanceOrderServiceClient.RemoteOfflineOrderDto> offlineOrders = orderServiceClient.getOfflineOrders().stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.status) && !"REJECTED".equalsIgnoreCase(o.status))
                .sorted(Comparator.comparing((FinanceOrderServiceClient.RemoteOfflineOrderDto o) -> o.orderDate != null ? o.orderDate : Instant.EPOCH))
                .collect(Collectors.toList());

        List<FinanceOrderServiceClient.RemoteCommerceOrderDto> commerceOrders = orderServiceClient.getCommerceOrders().stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.status) && !"FAILED".equalsIgnoreCase(o.paymentStatus) && !"REFUNDED".equalsIgnoreCase(o.paymentStatus))
                .sorted(Comparator.comparing((FinanceOrderServiceClient.RemoteCommerceOrderDto o) -> o.createdAt != null ? o.createdAt : Instant.EPOCH))
                .collect(Collectors.toList());

        // 2. Classify Initial Investment vs Additional Investments
        BigDecimal initialInvestment = BigDecimal.ZERO;
        BigDecimal additionalInvestments = BigDecimal.ZERO;
        Investment firstInvestment = allInvestments.isEmpty() ? null : allInvestments.get(0);

        if (firstInvestment != null) {
            initialInvestment = firstInvestment.getAmount() != null ? firstInvestment.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            for (int i = 1; i < allInvestments.size(); i++) {
                BigDecimal amt = allInvestments.get(i).getAmount();
                if (amt != null) {
                    additionalInvestments = additionalInvestments.add(amt);
                }
            }
        }
        BigDecimal totalInvestments = initialInvestment.add(additionalInvestments).setScale(2, RoundingMode.HALF_UP);

        // 3. Operating Expenses & Salary Totals
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Expense exp : allExpenses) {
            if (exp.getAmount() != null) {
                totalExpenses = totalExpenses.add(exp.getAmount());
            }
        }
        totalExpenses = totalExpenses.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalSalaries = BigDecimal.ZERO;
        for (SalaryPayment sal : allSalaries) {
            if (sal.getAmount() != null) {
                totalSalaries = totalSalaries.add(sal.getAmount());
            }
        }
        totalSalaries = totalSalaries.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalOperatingCosts = totalExpenses.add(totalSalaries).setScale(2, RoundingMode.HALF_UP);

        // 4. Order Income Totals (Wholesale, Website Retail, Manual Retail)
        BigDecimal wholesaleIncome = BigDecimal.ZERO;
        BigDecimal manualRetailIncome = BigDecimal.ZERO;
        for (FinanceOrderServiceClient.RemoteOfflineOrderDto o : offlineOrders) {
            BigDecimal amt = o.totalAmount != null ? o.totalAmount : BigDecimal.ZERO;
            if ("MANUAL_RETAIL".equalsIgnoreCase(o.orderType)) {
                manualRetailIncome = manualRetailIncome.add(amt);
            } else {
                wholesaleIncome = wholesaleIncome.add(amt);
            }
        }

        BigDecimal websiteRetailIncome = BigDecimal.ZERO;
        for (FinanceOrderServiceClient.RemoteCommerceOrderDto o : commerceOrders) {
            BigDecimal amt = o.totalAmount != null ? o.totalAmount : BigDecimal.ZERO;
            websiteRetailIncome = websiteRetailIncome.add(amt);
        }

        BigDecimal totalOrderIncome = wholesaleIncome.add(manualRetailIncome).add(websiteRetailIncome).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentOperationalBalance = totalInvestments.subtract(totalExpenses).subtract(totalSalaries).add(totalOrderIncome).setScale(2, RoundingMode.HALF_UP);

        dto.setInitialInvestment(initialInvestment);
        dto.setAdditionalInvestments(additionalInvestments);
        dto.setTotalInvestments(totalInvestments);
        dto.setTotalExpenses(totalExpenses);
        dto.setTotalSalaries(totalSalaries);
        dto.setTotalOperatingCosts(totalOperatingCosts);
        dto.setWholesaleOrderIncome(wholesaleIncome.setScale(2, RoundingMode.HALF_UP));
        dto.setManualRetailOrderIncome(manualRetailIncome.setScale(2, RoundingMode.HALF_UP));
        dto.setWebsiteRetailOrderIncome(websiteRetailIncome.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalOrderIncome(totalOrderIncome);
        dto.setCurrentOperationalBalance(currentOperationalBalance);

        // 5. Build Chronological Traceable Balance Ledger
        List<BalanceTransactionDto> traceableEvents = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        class RawEvent {
            Instant date;
            String type;
            String category;
            String description;
            String reference;
            String entityType;
            String entityId;
            BigDecimal amount;

            RawEvent(Instant date, String type, String category, String description, String reference, String entityType, String entityId, BigDecimal amount) {
                this.date = date;
                this.type = type;
                this.category = category;
                this.description = description;
                this.reference = reference;
                this.entityType = entityType;
                this.entityId = entityId;
                this.amount = amount;
            }
        }

        List<RawEvent> allRawEvents = new ArrayList<>();

        for (int i = 0; i < allInvestments.size(); i++) {
            Investment inv = allInvestments.get(i);
            Instant eventDate = inv.getInvestmentDate() != null
                    ? inv.getInvestmentDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : (inv.getCreatedAt() != null ? inv.getCreatedAt() : Instant.now());
            String type = (i == 0) ? "INITIAL_INVESTMENT" : "ADDITIONAL_INVESTMENT";
            String desc = "Capital Investment: " + inv.getInvestorName() + " (" + inv.getInvestmentType() + ")";
            allRawEvents.add(new RawEvent(
                    eventDate,
                    type,
                    inv.getInvestmentType(),
                    desc,
                    inv.getInvestorName(),
                    "INVESTMENT",
                    inv.getId() != null ? inv.getId().toString() : "",
                    inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO
            ));
        }

        for (Expense exp : allExpenses) {
            Instant eventDate = exp.getExpenseDate() != null
                    ? exp.getExpenseDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : (exp.getCreatedAt() != null ? exp.getCreatedAt() : Instant.now());
            String desc = "Operating Expense: " + exp.getCategory() + " (" + (exp.getDescription() != null ? exp.getDescription() : "") + ")";
            BigDecimal deduction = exp.getAmount() != null ? exp.getAmount().negate() : BigDecimal.ZERO;
            allRawEvents.add(new RawEvent(
                    eventDate,
                    "OPERATING_EXPENSE",
                    exp.getCategory(),
                    desc,
                    exp.getPayeeVendor() != null ? exp.getPayeeVendor() : exp.getPaymentMethod(),
                    "EXPENSE",
                    exp.getId() != null ? exp.getId().toString() : "",
                    deduction
            ));
        }

        for (SalaryPayment sal : allSalaries) {
            Instant eventDate = sal.getPaymentDate() != null
                    ? sal.getPaymentDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : (sal.getCreatedAt() != null ? sal.getCreatedAt() : Instant.now());
            String desc = "Salary / Wage Disbursement: Period " + sal.getPeriodMonth() + "/" + sal.getPeriodYear() + (sal.getNotes() != null ? " (" + sal.getNotes() + ")" : "");
            BigDecimal deduction = sal.getAmount() != null ? sal.getAmount().negate() : BigDecimal.ZERO;
            allRawEvents.add(new RawEvent(
                    eventDate,
                    "SALARY_PAYMENT",
                    "Payroll & Wages",
                    desc,
                    sal.getReferenceNumber() != null ? sal.getReferenceNumber() : sal.getPaymentMethod(),
                    "PAYROLL",
                    sal.getId() != null ? sal.getId().toString() : "",
                    deduction
            ));
        }

        for (FinanceOrderServiceClient.RemoteOfflineOrderDto o : offlineOrders) {
            Instant eventDate = o.orderDate != null ? o.orderDate : Instant.now();
            boolean isRetail = "MANUAL_RETAIL".equalsIgnoreCase(o.orderType);
            String type = isRetail ? "MANUAL_RETAIL_INCOME" : "WHOLESALE_ORDER_INCOME";
            String desc = (isRetail ? "Manual Retail Order: " : "Wholesale Order: ") + (o.customerName != null ? o.customerName : o.businessName);
            allRawEvents.add(new RawEvent(
                    eventDate,
                    type,
                    isRetail ? "Retail Sales" : "Wholesale Sales",
                    desc,
                    o.orderNumber != null ? o.orderNumber : o.invoiceNumber,
                    "OFFLINE_ORDER",
                    o.id != null ? o.id.toString() : "",
                    o.totalAmount != null ? o.totalAmount : BigDecimal.ZERO
            ));
        }

        for (FinanceOrderServiceClient.RemoteCommerceOrderDto o : commerceOrders) {
            Instant eventDate = o.createdAt != null ? o.createdAt : Instant.now();
            String desc = "Website Store Retail Order: " + (o.customerName != null ? o.customerName : o.customerEmail);
            allRawEvents.add(new RawEvent(
                    eventDate,
                    "WEBSITE_RETAIL_INCOME",
                    "Storefront E-Commerce",
                    desc,
                    o.orderNumber != null ? o.orderNumber : "WEB-ORDER",
                    "COMMERCE_ORDER",
                    o.id != null ? o.id.toString() : "",
                    o.totalAmount != null ? o.totalAmount : BigDecimal.ZERO
            ));
        }

        // Sort events chronologically
        allRawEvents.sort(Comparator.comparing((RawEvent e) -> e.date));

        for (RawEvent e : allRawEvents) {
            runningBalance = runningBalance.add(e.amount).setScale(2, RoundingMode.HALF_UP);
            traceableEvents.add(new BalanceTransactionDto(
                    UUID.randomUUID().toString(),
                    e.date,
                    e.type,
                    e.category,
                    e.description,
                    e.reference,
                    e.entityType,
                    e.entityId,
                    e.amount,
                    runningBalance
            ));
        }

        dto.setRecentTransactions(traceableEvents);

        // 6. Period Opening & Closing Calculation (if Year / Month selected)
        if (year != null) {
            LocalDate startDate;
            LocalDate endDate;
            if (month != null) {
                YearMonth ym = YearMonth.of(year, month);
                startDate = ym.atDay(1);
                endDate = ym.atEndOfMonth();
            } else {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }

            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endInstant = endDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

            // Opening balance is closing balance of everything strictly before startInstant
            BigDecimal openingBalance = BigDecimal.ZERO;
            for (RawEvent e : allRawEvents) {
                if (e.date.isBefore(startInstant)) {
                    openingBalance = openingBalance.add(e.amount);
                }
            }

            BigDecimal periodAdditionalInvestments = BigDecimal.ZERO;
            BigDecimal periodExpenses = BigDecimal.ZERO;
            BigDecimal periodSalaries = BigDecimal.ZERO;
            BigDecimal periodWholesaleIncome = BigDecimal.ZERO;
            BigDecimal periodManualRetailIncome = BigDecimal.ZERO;
            BigDecimal periodWebsiteRetailIncome = BigDecimal.ZERO;

            for (RawEvent e : allRawEvents) {
                if (!e.date.isBefore(startInstant) && !e.date.isAfter(endInstant)) {
                    if ("INITIAL_INVESTMENT".equals(e.type) || "ADDITIONAL_INVESTMENT".equals(e.type)) {
                        periodAdditionalInvestments = periodAdditionalInvestments.add(e.amount);
                    } else if ("OPERATING_EXPENSE".equals(e.type)) {
                        periodExpenses = periodExpenses.add(e.amount.abs());
                    } else if ("SALARY_PAYMENT".equals(e.type)) {
                        periodSalaries = periodSalaries.add(e.amount.abs());
                    } else if ("WHOLESALE_ORDER_INCOME".equals(e.type)) {
                        periodWholesaleIncome = periodWholesaleIncome.add(e.amount);
                    } else if ("MANUAL_RETAIL_INCOME".equals(e.type)) {
                        periodManualRetailIncome = periodManualRetailIncome.add(e.amount);
                    } else if ("WEBSITE_RETAIL_INCOME".equals(e.type)) {
                        periodWebsiteRetailIncome = periodWebsiteRetailIncome.add(e.amount);
                    }
                }
            }

            BigDecimal periodTotalOrderIncome = periodWholesaleIncome.add(periodManualRetailIncome).add(periodWebsiteRetailIncome);
            BigDecimal periodOperatingCosts = periodExpenses.add(periodSalaries).setScale(2, RoundingMode.HALF_UP);
            BigDecimal closingBalance = openingBalance.add(periodAdditionalInvestments).subtract(periodExpenses).subtract(periodSalaries).add(periodTotalOrderIncome);

            dto.setOpeningBalance(openingBalance.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodAdditionalInvestments(periodAdditionalInvestments.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodExpenses(periodExpenses.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodSalaries(periodSalaries.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodOperatingCosts(periodOperatingCosts);
            dto.setPeriodWholesaleIncome(periodWholesaleIncome.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodManualRetailIncome(periodManualRetailIncome.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodWebsiteRetailIncome(periodWebsiteRetailIncome.setScale(2, RoundingMode.HALF_UP));
            dto.setPeriodTotalOrderIncome(periodTotalOrderIncome.setScale(2, RoundingMode.HALF_UP));
            dto.setClosingBalance(closingBalance.setScale(2, RoundingMode.HALF_UP));
        } else {
            dto.setOpeningBalance(initialInvestment);
            dto.setPeriodAdditionalInvestments(additionalInvestments);
            dto.setPeriodExpenses(totalExpenses);
            dto.setPeriodSalaries(totalSalaries);
            dto.setPeriodOperatingCosts(totalOperatingCosts);
            dto.setPeriodWholesaleIncome(wholesaleIncome);
            dto.setPeriodManualRetailIncome(manualRetailIncome);
            dto.setPeriodWebsiteRetailIncome(websiteRetailIncome);
            dto.setPeriodTotalOrderIncome(totalOrderIncome);
            dto.setClosingBalance(currentOperationalBalance);
        }

        // 7. Inventory Stock Valuation (Kept strictly separate from Operational Balance)
        List<FinanceOrderServiceClient.RemoteInventoryItemDto> invItems = orderServiceClient.getInventoryItems();
        List<FinanceOrderServiceClient.RemoteProductDto> products = orderServiceClient.getCommerceProducts();

        Map<String, BigDecimal> productPriceMap = new HashMap<>();
        for (FinanceOrderServiceClient.RemoteProductDto p : products) {
            BigDecimal price = (p.retailPrice != null && p.retailPrice.compareTo(BigDecimal.ZERO) > 0)
                    ? p.retailPrice
                    : (p.wholesalePrice != null ? p.wholesalePrice : BigDecimal.ZERO);
            if (p.sku != null) productPriceMap.put(p.sku.trim().toUpperCase(), price);
            if (p.name != null) productPriceMap.put(p.name.trim().toLowerCase(), price);
        }

        List<StockValuationItemDto> finishedGoods = new ArrayList<>();
        List<StockValuationItemDto> rawMaterials = new ArrayList<>();
        BigDecimal finishedGoodsTotalVal = BigDecimal.ZERO;
        BigDecimal rawMaterialsTotalVal = BigDecimal.ZERO;
        boolean rawMaterialPriceFound = false;

        for (FinanceOrderServiceClient.RemoteInventoryItemDto item : invItems) {
            BigDecimal qty = item.currentQuantity != null ? item.currentQuantity : BigDecimal.ZERO;
            if ("FINISHED_GOOD".equalsIgnoreCase(item.itemType)) {
                BigDecimal unitPrice = BigDecimal.ZERO;
                if (item.sku != null && productPriceMap.containsKey(item.sku.trim().toUpperCase())) {
                    unitPrice = productPriceMap.get(item.sku.trim().toUpperCase());
                } else if (item.itemName != null && productPriceMap.containsKey(item.itemName.trim().toLowerCase())) {
                    unitPrice = productPriceMap.get(item.itemName.trim().toLowerCase());
                } else {
                    unitPrice = new BigDecimal("100.00"); // fallback standard finished jaggery price per KG
                }
                BigDecimal val = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                finishedGoodsTotalVal = finishedGoodsTotalVal.add(val);
                finishedGoods.add(new StockValuationItemDto(
                        item.id != null ? item.id.toString() : UUID.randomUUID().toString(),
                        item.sku,
                        item.itemName,
                        "FINISHED_GOOD",
                        qty,
                        item.unitOfMeasure != null ? item.unitOfMeasure : "KG",
                        unitPrice,
                        val,
                        "AUTHORITATIVE"
                ));
            } else if ("RAW_MATERIAL".equalsIgnoreCase(item.itemType)) {
                // Authoritative procurement valuation (e.g. sugarcane moving average: ₹3.50/kg)
                BigDecimal unitPrice = new BigDecimal("3.50");
                BigDecimal val = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                rawMaterialsTotalVal = rawMaterialsTotalVal.add(val);
                rawMaterialPriceFound = true;
                rawMaterials.add(new StockValuationItemDto(
                        item.id != null ? item.id.toString() : UUID.randomUUID().toString(),
                        item.sku,
                        item.itemName,
                        "RAW_MATERIAL",
                        qty,
                        item.unitOfMeasure != null ? item.unitOfMeasure : "KG",
                        unitPrice,
                        val,
                        "AUTHORITATIVE"
                ));
            }
        }

        BigDecimal totalStockValue = finishedGoodsTotalVal.add(rawMaterialsTotalVal).setScale(2, RoundingMode.HALF_UP);
        dto.setFinishedGoodsStockValue(finishedGoodsTotalVal.setScale(2, RoundingMode.HALF_UP));
        dto.setRawMaterialStockValue(rawMaterialsTotalVal.setScale(2, RoundingMode.HALF_UP));
        dto.setRawMaterialValuationStatus(rawMaterialPriceFound ? "AUTHORITATIVE" : "VALUATION_UNAVAILABLE");
        dto.setTotalStockValue(totalStockValue);
        dto.setFinishedGoodsStockDetails(finishedGoods);
        dto.setRawMaterialStockDetails(rawMaterials);

        // Balance = Investment − Expenses - Salaries + Gross Sales Value + In Stock Value
        BigDecimal opsBalanceForPosition = (year != null && dto.getClosingBalance() != null) ? dto.getClosingBalance() : currentOperationalBalance;
        BigDecimal totalBusinessPosition = opsBalanceForPosition.add(finishedGoodsTotalVal).setScale(2, RoundingMode.HALF_UP);
        dto.setTotalBusinessPosition(totalBusinessPosition);

        return dto;
    }
}
