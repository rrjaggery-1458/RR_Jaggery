package com.rrjaggery.common.forms.repository;

import com.rrjaggery.common.forms.entity.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {
    List<FormSubmission> findByRecordTypeAndRecordIdOrderBySubmittedAtDesc(String recordType, UUID recordId);
}
