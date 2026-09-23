package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID> {

    List<CustomerPayment> findByCustomerIdOrderByPaymentDateDesc(UUID customerId);

    Optional<CustomerPayment> findByPaymentNumber(String paymentNumber);

    Optional<CustomerPayment> findByIdempotencyKey(String idempotencyKey);
}
