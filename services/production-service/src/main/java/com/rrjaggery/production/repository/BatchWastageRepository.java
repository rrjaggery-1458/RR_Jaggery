package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.BatchWastage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchWastageRepository extends JpaRepository<BatchWastage, UUID> {
    List<BatchWastage> findByBatchId(UUID batchId);
    Optional<BatchWastage> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
