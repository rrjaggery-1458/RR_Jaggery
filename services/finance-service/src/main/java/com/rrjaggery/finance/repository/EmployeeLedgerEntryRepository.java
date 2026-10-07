package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.EmployeeLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeLedgerEntryRepository extends JpaRepository<EmployeeLedgerEntry, UUID> {
    List<EmployeeLedgerEntry> findByEmployeeIdOrderByTransactionDateDescCreatedAtDesc(UUID employeeId);
    List<EmployeeLedgerEntry> findByEmployeeIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(UUID employeeId, LocalDate startDate, LocalDate endDate);
    List<EmployeeLedgerEntry> findAllByOrderByTransactionDateDescCreatedAtDesc();
}
