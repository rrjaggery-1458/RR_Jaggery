package com.rrjaggery.common.forms.repository;

import com.rrjaggery.common.forms.entity.FormRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormRevisionRepository extends JpaRepository<FormRevision, UUID> {
    Optional<FormRevision> findByFormDefinitionIdAndVersion(UUID formDefinitionId, int version);
}
