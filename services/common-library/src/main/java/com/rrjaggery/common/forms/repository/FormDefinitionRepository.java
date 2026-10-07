package com.rrjaggery.common.forms.repository;

import com.rrjaggery.common.forms.entity.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {
    Optional<FormDefinition> findByFormKey(String formKey);
    List<FormDefinition> findAllByOrderByDisplayNameAsc();
    List<FormDefinition> findByFormKeyInOrderByDisplayNameAsc(List<String> formKeys);
}
