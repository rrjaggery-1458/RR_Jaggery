package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.StockOutboxCommand;
import com.rrjaggery.customerledger.entity.StockOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockOutboxRepository extends JpaRepository<StockOutboxCommand, Long> {
    Optional<StockOutboxCommand> findByCommandKey(String commandKey);
    List<StockOutboxCommand> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
            StockOutboxStatus status, Instant now);
}
