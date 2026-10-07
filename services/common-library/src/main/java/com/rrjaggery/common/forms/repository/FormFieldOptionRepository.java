package com.rrjaggery.common.forms.repository;

import com.rrjaggery.common.forms.entity.FormFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormFieldOptionRepository extends JpaRepository<FormFieldOption, UUID> {
    List<FormFieldOption> findByFieldIdOrderByDisplayOrderAsc(UUID fieldId);
}
