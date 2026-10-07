package com.rrjaggery.common.forms.repository;

import com.rrjaggery.common.forms.entity.FormField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormFieldRepository extends JpaRepository<FormField, UUID> {
    List<FormField> findByFormDefinitionIdOrderByDisplayOrderAsc(UUID formDefinitionId);
    Optional<FormField> findByFormDefinitionIdAndFieldKey(UUID formDefinitionId, String fieldKey);
}
