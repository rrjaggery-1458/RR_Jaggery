package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.BatchStatus;
import com.rrjaggery.production.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, UUID> {
    Optional<ProductionBatch> findByBatchNumber(String batchNumber);
    Optional<ProductionBatch> findByBatchNumberIgnoreCase(String batchNumber);
    List<ProductionBatch> findByStatus(BatchStatus status);
    List<ProductionBatch> findByTargetProductSku(String targetProductSku);
    List<ProductionBatch> findByBatchLot(String batchLot);
    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    /**
     * Returns batches whose effective date falls within [periodStart, periodEnd).
     * Effective date priority: completionDate → actualStartDate → plannedStartDate → createdAt.
     */
    @Query("""
        SELECT b FROM ProductionBatch b
        WHERE (
            CASE
                WHEN b.completionDate IS NOT NULL THEN b.completionDate
                WHEN b.actualStartDate IS NOT NULL THEN b.actualStartDate
                WHEN b.plannedStartDate IS NOT NULL THEN b.plannedStartDate
                ELSE b.createdAt
            END
        ) >= :periodStart
        AND (
            CASE
                WHEN b.completionDate IS NOT NULL THEN b.completionDate
                WHEN b.actualStartDate IS NOT NULL THEN b.actualStartDate
                WHEN b.plannedStartDate IS NOT NULL THEN b.plannedStartDate
                ELSE b.createdAt
            END
        ) < :periodEnd
        ORDER BY b.createdAt DESC
        """)
    List<ProductionBatch> findByPeriod(@Param("periodStart") Instant periodStart,
                                       @Param("periodEnd") Instant periodEnd);

    @Query("""
        SELECT b FROM ProductionBatch b
        WHERE b.status = :status
        AND (
            CASE
                WHEN b.completionDate IS NOT NULL THEN b.completionDate
                WHEN b.actualStartDate IS NOT NULL THEN b.actualStartDate
                WHEN b.plannedStartDate IS NOT NULL THEN b.plannedStartDate
                ELSE b.createdAt
            END
        ) >= :periodStart
        AND (
            CASE
                WHEN b.completionDate IS NOT NULL THEN b.completionDate
                WHEN b.actualStartDate IS NOT NULL THEN b.actualStartDate
                WHEN b.plannedStartDate IS NOT NULL THEN b.plannedStartDate
                ELSE b.createdAt
            END
        ) < :periodEnd
        ORDER BY b.createdAt DESC
        """)
    List<ProductionBatch> findByStatusAndPeriod(@Param("status") BatchStatus status,
                                                @Param("periodStart") Instant periodStart,
                                                @Param("periodEnd") Instant periodEnd);
}
