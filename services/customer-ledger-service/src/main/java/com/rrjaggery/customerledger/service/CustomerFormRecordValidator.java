package com.rrjaggery.customerledger.service;

import com.rrjaggery.common.forms.service.FormRecordValidator;
import com.rrjaggery.customerledger.repository.OfflineOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class CustomerFormRecordValidator implements FormRecordValidator {
    private final OfflineOrderRepository offlineOrderRepository;

    public CustomerFormRecordValidator(OfflineOrderRepository offlineOrderRepository) {
        this.offlineOrderRepository = offlineOrderRepository;
    }

    @Override
    public boolean supports(String recordType) {
        return "WHOLESALE_ORDER".equals(recordType);
    }

    @Override
    public void requireExistingRecord(String recordType, UUID recordId) {
        if (!supports(recordType) || !offlineOrderRepository.existsById(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked wholesale order not found");
        }
    }
}
