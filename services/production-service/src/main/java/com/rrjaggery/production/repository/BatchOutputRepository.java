package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.BatchOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchOutputRepository extends JpaRepository<BatchOutput, UUID> {
    List<BatchOutput> findByBatchId(UUID batchId);
    Optional<BatchOutput> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
