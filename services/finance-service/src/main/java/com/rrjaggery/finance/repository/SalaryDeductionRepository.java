package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.SalaryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryDeductionRepository extends JpaRepository<SalaryDeduction, UUID> {
    List<SalaryDeduction> findByEmployeeIdOrderByDeductionDateDesc(UUID employeeId);
    List<SalaryDeduction> findByEmployeeIdAndPeriodYearAndPeriodMonth(UUID employeeId, int periodYear, int periodMonth);
    List<SalaryDeduction> findByPeriodYearAndPeriodMonthOrderByDeductionDateDesc(int periodYear, int periodMonth);
    List<SalaryDeduction> findAllByOrderByDeductionDateDesc();
}
