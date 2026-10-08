package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.CustomerLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedgerEntry, UUID> {

    List<CustomerLedgerEntry> findByCustomerIdOrderByTransactionDateAsc(UUID customerId);

    List<CustomerLedgerEntry> findByCustomerIdOrderByTransactionDateDesc(UUID customerId);

    Optional<CustomerLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT l FROM CustomerLedgerEntry l WHERE l.customer.id = :customerId AND l.transactionDate BETWEEN :from AND :to ORDER BY l.transactionDate ASC")
    List<CustomerLedgerEntry> findCustomerStatement(
            @Param("customerId") UUID customerId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
