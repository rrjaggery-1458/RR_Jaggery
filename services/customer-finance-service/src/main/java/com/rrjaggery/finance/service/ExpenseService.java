package com.rrjaggery.finance.service;

import com.rrjaggery.finance.dto.CategoryExpenseDto;
import com.rrjaggery.finance.dto.CreateExpenseRequest;
import com.rrjaggery.finance.dto.UpdateExpenseRequest;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpenses(Integer year, Integer month, String category) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else if (year != null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        if (startDate != null && endDate != null) {
            if (category != null && !category.isBlank()) {
                return expenseRepository.findByCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDesc(category.trim(), startDate, endDate);
            }
            return expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(startDate, endDate);
        }

        if (category != null && !category.isBlank()) {
            return expenseRepository.findByCategoryIgnoreCaseOrderByExpenseDateDesc(category.trim());
        }

        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    @Transactional(readOnly = true)
    public Expense getExpenseById(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found for ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseDto> getExpenseBreakdown(Integer year, Integer month) {
        LocalDate startDate;
        LocalDate endDate;

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else if (year != null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            startDate = LocalDate.of(2000, 1, 1);
            endDate = LocalDate.of(2099, 12, 31);
        }

        List<Object[]> rows = expenseRepository.getCategoryExpenseSummary(startDate, endDate);
        BigDecimal total = BigDecimal.ZERO;

        for (Object[] row : rows) {
            BigDecimal amt = (BigDecimal) row[1];
            if (amt != null) {
                total = total.add(amt);
            }
        }

        List<CategoryExpenseDto> dtos = new ArrayList<>();
        for (Object[] row : rows) {
            String category = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            double pct = 0.0;
            if (total.compareTo(BigDecimal.ZERO) > 0 && amt != null) {
                pct = amt.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            dtos.add(new CategoryExpenseDto(category, amt != null ? amt : BigDecimal.ZERO, Math.round(pct * 10.0) / 10.0));
        }

        return dtos;
    }

    @Transactional
    public Expense createExpense(CreateExpenseRequest request) {
        Expense expense = new Expense();
        expense.setExpenseDate(request.getExpenseDate() != null
                ? request.getExpenseDate()
                : LocalDate.now(ZoneId.of("Asia/Kolkata")));
        expense.setCategory(request.getCategory().trim());
        expense.setAmount(request.getAmount());
        expense.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        expense.setPayeeVendor(request.getPayeeVendor());
        expense.setDescription(request.getDescription());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setReceiptUrl(request.getReceiptUrl());
        expense.setNotes(request.getNotes());
        expense.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "MANAGER");
        expense.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : "PAID");
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(UUID id, UpdateExpenseRequest request) {
        Expense expense = getExpenseById(id);
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            expense.setCategory(request.getCategory().trim());
        }
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getPaymentMethod() != null) {
            expense.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getPayeeVendor() != null) {
            expense.setPayeeVendor(request.getPayeeVendor());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getReferenceNumber() != null) {
            expense.setReferenceNumber(request.getReferenceNumber());
        }
        if (request.getReceiptUrl() != null) {
            expense.setReceiptUrl(request.getReceiptUrl());
        }
        if (request.getNotes() != null) {
            expense.setNotes(request.getNotes());
        }
        if (request.getPaymentStatus() != null) {
            expense.setPaymentStatus(request.getPaymentStatus());
        }
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        expenseRepository.deleteById(id);
    }
}
