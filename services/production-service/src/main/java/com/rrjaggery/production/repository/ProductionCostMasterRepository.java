package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.ProductionCostMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionCostMasterRepository extends JpaRepository<ProductionCostMaster, UUID> {

    List<ProductionCostMaster> findByActiveTrueOrderByCostNameAsc();

    @Query("SELECT p FROM ProductionCostMaster p WHERE p.active = true AND p.effectiveFrom <= :date AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date) ORDER BY p.costName ASC")
    List<ProductionCostMaster> findActiveCostsForDate(@Param("date") LocalDate date);
}
