package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    List<Investment> findAllByOrderByInvestmentDateDescCreatedAtDesc();
}
