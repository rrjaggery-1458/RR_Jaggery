package com.rrjaggery.procurement.repository;

import com.rrjaggery.procurement.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    boolean existsBySupplierCode(String supplierCode);
}
