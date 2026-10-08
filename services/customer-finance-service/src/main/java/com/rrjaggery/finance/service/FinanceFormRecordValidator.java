package com.rrjaggery.finance.service;

import com.rrjaggery.common.forms.service.FormRecordValidator;
import com.rrjaggery.finance.repository.AttendanceRepository;
import com.rrjaggery.finance.repository.EmployeeRepository;
import com.rrjaggery.finance.repository.ExpenseRepository;
import com.rrjaggery.finance.repository.InvestmentRepository;
import com.rrjaggery.finance.repository.PayrollRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@Component
public class FinanceFormRecordValidator implements FormRecordValidator {
    private final Map<String, Predicate<UUID>> recordExists;

    public FinanceFormRecordValidator(
            ExpenseRepository expenses,
            InvestmentRepository investments,
            EmployeeRepository employees,
            AttendanceRepository attendance,
            PayrollRecordRepository payrollRecords) {
        recordExists = Map.of(
                "EXPENSE", expenses::existsById,
                "INVESTMENT", investments::existsById,
                "STAFF", employees::existsById,
                "ATTENDANCE", attendance::existsById,
                "PAYROLL", payrollRecords::existsById);
    }

    @Override
    public boolean supports(String recordType) {
        return recordExists.containsKey(recordType);
    }

    @Override
    public void requireExistingRecord(String recordType, UUID recordId) {
        Predicate<UUID> exists = recordExists.get(recordType);
        if (exists == null || !exists.test(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked finance record not found");
        }
    }
}
