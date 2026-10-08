package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, UUID> {
    List<PayrollRecord> findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(int periodYear, int periodMonth);
    List<PayrollRecord> findByEmployeeIdOrderByPaymentDateDesc(UUID employeeId);
    List<PayrollRecord> findAllByOrderByPaymentDateDesc();
}
