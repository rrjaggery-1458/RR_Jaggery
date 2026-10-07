package com.rrjaggery.production.service;

import com.rrjaggery.common.forms.service.FormRecordValidator;
import com.rrjaggery.production.repository.ProductionBatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class ProductionFormRecordValidator implements FormRecordValidator {
    private final ProductionBatchRepository productionBatchRepository;

    public ProductionFormRecordValidator(ProductionBatchRepository productionBatchRepository) {
        this.productionBatchRepository = productionBatchRepository;
    }

    @Override
    public boolean supports(String recordType) {
        return "PRODUCTION".equals(recordType);
    }

    @Override
    public void requireExistingRecord(String recordType, UUID recordId) {
        if (!supports(recordType) || !productionBatchRepository.existsById(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked production batch not found");
        }
    }
}
