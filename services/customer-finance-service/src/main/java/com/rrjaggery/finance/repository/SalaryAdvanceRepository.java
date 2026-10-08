package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, UUID> {
    List<SalaryAdvance> findByEmployeeIdOrderByAdvanceDateDesc(UUID employeeId);
    List<SalaryAdvance> findByEmployeeIdAndAdvanceDateBetweenOrderByAdvanceDateDesc(UUID employeeId, LocalDate startDate, LocalDate endDate);
    List<SalaryAdvance> findByEmployeeIdAndStatus(UUID employeeId, String status);
    List<SalaryAdvance> findAllByOrderByAdvanceDateDesc();
}
