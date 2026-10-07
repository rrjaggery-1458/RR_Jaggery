package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.PayrollRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRuleRepository extends JpaRepository<PayrollRule, UUID> {
    List<PayrollRule> findByActiveTrueOrderByRuleNameAsc();

    @Query("SELECT r FROM PayrollRule r WHERE r.active = true AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) ORDER BY r.effectiveFrom DESC")
    List<PayrollRule> findActiveRulesForDate(@Param("date") LocalDate date);
}
