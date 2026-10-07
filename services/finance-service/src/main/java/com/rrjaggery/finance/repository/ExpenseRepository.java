package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate startDate, LocalDate endDate);
    List<Expense> findByCategoryIgnoreCaseOrderByExpenseDateDesc(String category);
    List<Expense> findByCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDesc(String category, LocalDate startDate, LocalDate endDate);
    List<Expense> findAllByOrderByExpenseDateDesc();

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategoryExpenseSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
