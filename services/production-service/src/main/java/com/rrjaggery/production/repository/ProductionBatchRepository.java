package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.BatchStatus;
import com.rrjaggery.production.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
