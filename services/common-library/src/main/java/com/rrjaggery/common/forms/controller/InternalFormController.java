package com.rrjaggery.common.forms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.common.forms.dto.FormDefinitionRequest;
import com.rrjaggery.common.forms.dto.FormDefinitionView;
import com.rrjaggery.common.forms.dto.FormFieldRequest;
import com.rrjaggery.common.forms.dto.FormFieldUpdateRequest;
import com.rrjaggery.common.forms.dto.FormFieldView;
import com.rrjaggery.common.forms.dto.FormOptionRequest;
import com.rrjaggery.common.forms.dto.FormOptionView;
import com.rrjaggery.common.forms.dto.FormStatusRequest;
import com.rrjaggery.common.forms.dto.FormSubmissionRequest;
import com.rrjaggery.common.forms.dto.FormSubmissionView;
import com.rrjaggery.common.forms.service.FormRegistryService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"${app.forms.api-base:/api/v1/forms}", "${app.forms.secondary-api-base:/internal/forms/disabled-secondary}"})
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@ConditionalOnProperty(prefix = "app.forms", name = "enabled", havingValue = "true")
public class InternalFormController {
    private final FormRegistryService formRegistryService;

    public InternalFormController(FormRegistryService formRegistryService) {
        this.formRegistryService = formRegistryService;
    }

    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<FormDefinitionView>>> listDefinitions(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.listDefinitions(admin)));
    }

    @GetMapping("/definitions/{formKey}")
    public ResponseEntity<ApiResponse<FormDefinitionView>> getDefinition(
            @PathVariable String formKey,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.getDefinition(formKey, isAdmin(authentication))));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormDefinitionView>> createDefinition(
            @Valid @RequestBody FormDefinitionRequest request) {
        FormDefinitionView created = formRegistryService.createDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Form definition created."));
    }

    @PutMapping("/definitions/{formKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormDefinitionView>> updateDefinition(
            @PathVariable String formKey,
            @Valid @RequestBody FormDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.updateDefinition(formKey, request)));
    }

    @PatchMapping("/definitions/{formKey}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormDefinitionView>> setDefinitionStatus(
            @PathVariable String formKey,
            @Valid @RequestBody FormStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.setDefinitionStatus(formKey, request)));
    }

    @PostMapping("/definitions/{formKey}/fields")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldView>> addField(
            @PathVariable String formKey,
            @Valid @RequestBody FormFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(formRegistryService.addField(formKey, request), "Form field created."));
    }

    @PutMapping("/fields/{fieldId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldView>> updateField(
            @PathVariable UUID fieldId,
            @Valid @RequestBody FormFieldUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.updateField(fieldId, request)));
    }

    @PatchMapping("/fields/{fieldId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldView>> setFieldStatus(
            @PathVariable UUID fieldId,
            @Valid @RequestBody FormStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.setFieldStatus(fieldId, request)));
    }

    @PatchMapping("/definitions/{formKey}/fields/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FormFieldView>>> reorderFields(
            @PathVariable String formKey,
            @RequestBody List<UUID> orderedFieldIds) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.reorderFields(formKey, orderedFieldIds)));
    }

    @PostMapping("/fields/{fieldId}/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormOptionView>> addOption(
            @PathVariable UUID fieldId,
            @Valid @RequestBody FormOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(formRegistryService.addOption(fieldId, request), "Field option created."));
    }

    @PutMapping("/field-options/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormOptionView>> updateOption(
            @PathVariable UUID optionId,
            @Valid @RequestBody FormOptionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.updateOption(optionId, request)));
    }

    @PatchMapping("/field-options/{optionId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormOptionView>> setOptionStatus(
            @PathVariable UUID optionId,
            @Valid @RequestBody FormStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.setOptionStatus(optionId, request)));
    }

    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<FormSubmissionView>> submit(
            @Valid @RequestBody FormSubmissionRequest request) {
        FormSubmissionView saved = formRegistryService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(saved, "Supplemental form data saved."));
    }

    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<FormSubmissionView>>> getSubmissions(
            @RequestParam String recordType,
            @RequestParam UUID recordId) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.getSubmissions(recordType, recordId)));
    }

    @GetMapping("/definitions/{formKey}/revisions/{version}")
    public ResponseEntity<ApiResponse<JsonNode>> getRevision(
            @PathVariable String formKey,
            @PathVariable int version) {
        return ResponseEntity.ok(ApiResponse.ok(formRegistryService.getRevision(formKey, version)));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
