package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    List<ExpenseCategory> findByActiveTrueOrderByNameAsc();
    Optional<ExpenseCategory> findByNameIgnoreCase(String name);
}
