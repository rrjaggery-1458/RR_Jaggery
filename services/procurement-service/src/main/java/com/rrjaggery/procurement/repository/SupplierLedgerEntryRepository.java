package com.rrjaggery.procurement.repository;

import com.rrjaggery.procurement.entity.Supplier;
import com.rrjaggery.procurement.entity.SupplierLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierLedgerEntryRepository extends JpaRepository<SupplierLedgerEntry, UUID> {
    List<SupplierLedgerEntry> findBySupplierOrderByCreatedAtDesc(Supplier supplier);
}
