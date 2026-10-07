package com.rrjaggery.common.forms.service;

import java.util.UUID;

public interface FormRecordValidator {
    boolean supports(String recordType);
    void requireExistingRecord(String recordType, UUID recordId);
}
