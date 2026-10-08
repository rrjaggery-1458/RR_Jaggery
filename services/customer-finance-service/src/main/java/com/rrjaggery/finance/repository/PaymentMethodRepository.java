package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByActiveTrueOrderByDisplayNameAsc();
    Optional<PaymentMethod> findByCodeIgnoreCase(String code);
}
