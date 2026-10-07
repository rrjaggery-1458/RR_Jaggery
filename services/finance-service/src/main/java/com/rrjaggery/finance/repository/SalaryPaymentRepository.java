package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, UUID> {
    List<SalaryPayment> findByEmployeeIdOrderByPaymentDateDesc(UUID employeeId);
    List<SalaryPayment> findByEmployeeIdAndPeriodYearAndPeriodMonth(UUID employeeId, int periodYear, int periodMonth);
    List<SalaryPayment> findByPeriodYearAndPeriodMonthOrderByPaymentDateDesc(int periodYear, int periodMonth);
    List<SalaryPayment> findAllByOrderByPaymentDateDesc();
}
