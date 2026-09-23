package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.BatchConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchConsumptionRepository extends JpaRepository<BatchConsumption, UUID> {
    List<BatchConsumption> findByBatchId(UUID batchId);
    Optional<BatchConsumption> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
