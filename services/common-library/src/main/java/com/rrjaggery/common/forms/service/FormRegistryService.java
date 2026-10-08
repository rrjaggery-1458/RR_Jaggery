package com.rrjaggery.common.forms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.common.forms.FieldType;
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
import com.rrjaggery.common.forms.entity.FormDefinition;
import com.rrjaggery.common.forms.entity.FormField;
import com.rrjaggery.common.forms.entity.FormFieldOption;
import com.rrjaggery.common.forms.entity.FormRevision;
import com.rrjaggery.common.forms.entity.FormSubmission;
import com.rrjaggery.common.forms.repository.FormDefinitionRepository;
import com.rrjaggery.common.forms.repository.FormFieldOptionRepository;
import com.rrjaggery.common.forms.repository.FormFieldRepository;
import com.rrjaggery.common.forms.repository.FormRevisionRepository;
import com.rrjaggery.common.forms.repository.FormSubmissionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "app.forms", name = "enabled", havingValue = "true")
public class FormRegistryService {
    private static final Set<String> MANAGER_RESTRICTED_FORMS = Set.of("INVESTMENT", "STAFF");
    private static final Set<String> CORE_FORM_KEYS = Set.of(
            "EXPENSE", "INVESTMENT", "WHOLESALE_ORDER", "PRODUCTION", "STAFF",
            "ATTENDANCE", "PAYROLL", "SUPPLIER", "PROCUREMENT", "STOCK", "PRODUCT", "CUSTOMER");
    private static final Pattern FORM_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,79}$");
    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9_]{0,99}$");
    private static final Set<String> VALIDATION_KEYS = Set.of("min", "max", "maxLength", "scale");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{7,25}$");

    private final FormDefinitionRepository definitionRepository;
    private final FormFieldRepository fieldRepository;
    private final FormFieldOptionRepository optionRepository;
    private final FormRevisionRepository revisionRepository;
    private final FormSubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;
    private final Set<String> ownedFormKeys;
    private final List<FormRecordValidator> recordValidators;

    public FormRegistryService(
            FormDefinitionRepository definitionRepository,
            FormFieldRepository fieldRepository,
            FormFieldOptionRepository optionRepository,
            FormRevisionRepository revisionRepository,
            FormSubmissionRepository submissionRepository,
            ObjectMapper objectMapper,
            @Value("${app.forms.owned-keys:}") String ownedFormKeys,
            ObjectProvider<FormRecordValidator> recordValidators) {
        this.definitionRepository = definitionRepository;
        this.fieldRepository = fieldRepository;
        this.optionRepository = optionRepository;
        this.revisionRepository = revisionRepository;
        this.submissionRepository = submissionRepository;
        this.objectMapper = objectMapper;
        this.ownedFormKeys = Arrays.stream(ownedFormKeys.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.recordValidators = recordValidators.orderedStream().toList();
    }

    @PostConstruct
    void validateConfiguredFormKeys() {
        if (ownedFormKeys.stream().anyMatch(key -> !FORM_KEY_PATTERN.matcher(key).matches())) {
            throw new IllegalStateException("app.forms.owned-keys contains an invalid technical form key");
        }
    }

    @Transactional
    public void ensureOwnedFormsExist() {
        for (String key : ownedFormKeys) {
            if (definitionRepository.findByFormKey(key).isPresent()) {
                continue;
            }
            FormDefinition definition = new FormDefinition();
            definition.setFormKey(key);
            definition.setDisplayName(defaultDisplayName(key));
            definition.setDescription("Admin-configurable " + defaultDisplayName(key) + " form.");
            definition.setCreatedBy("system");
            definition.setUpdatedBy("system");
            definition = definitionRepository.save(definition);
            createRevision(definition, "system");
        }
        if (ownedFormKeys.contains("INVESTMENT")) {
            ensureInvestmentTypeField();
        }
    }

    @Transactional(readOnly = true)
    public List<FormDefinitionView> listDefinitions(boolean admin) {
        List<FormDefinition> definitions = definitionRepository.findAllByOrderByDisplayNameAsc();
        return definitions.stream()
                .filter(definition -> admin || (definition.isActive() && managerMayUse(definition.getFormKey())))
                .map(definition -> toView(definition, admin))
                .toList();
    }

    @Transactional(readOnly = true)
    public FormDefinitionView getDefinition(String formKey, boolean admin) {
        FormDefinition definition = getOwnedDefinition(formKey);
        if (!admin && (!definition.isActive() || !managerMayUse(formKey))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found");
        }
        return toView(definition, admin);
    }

    @Transactional
    public FormDefinitionView createDefinition(FormDefinitionRequest request) {
        validateFormKey(request.formKey());
        requireOwnedFormKey(request.formKey());
        if (definitionRepository.findByFormKey(request.formKey()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Form key already exists");
        }
        String actor = currentActor();
        FormDefinition definition = new FormDefinition();
        definition.setFormKey(request.formKey());
        definition.setDisplayName(request.displayName().trim());
        definition.setDescription(trimToNull(request.description()));
        definition.setCreatedBy(actor);
        definition.setUpdatedBy(actor);
        definition = definitionRepository.save(definition);
        saveRevision(definition, actor);
        return toView(definition, true);
    }

    @Transactional
    public FormDefinitionView updateDefinition(String formKey, FormDefinitionRequest request) {
        FormDefinition definition = getOwnedDefinition(formKey);
        if (!formKey.equals(request.formKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technical form key cannot be changed");
        }
        String actor = currentActor();
        definition.setDisplayName(request.displayName().trim());
        definition.setDescription(trimToNull(request.description()));
        definition.setUpdatedBy(actor);
        definition.setVersion(definition.getVersion() + 1);
        definitionRepository.save(definition);
        saveRevision(definition, actor);
        return toView(definition, true);
    }

    @Transactional
    public FormDefinitionView setDefinitionStatus(String formKey, FormStatusRequest request) {
        FormDefinition definition = getOwnedDefinition(formKey);
        String actor = currentActor();
        definition.setActive(request.active());
        definition.setUpdatedBy(actor);
        definition.setVersion(definition.getVersion() + 1);
        definitionRepository.save(definition);
        saveRevision(definition, actor);
        return toView(definition, true);
    }

    @Transactional
    public FormFieldView addField(String formKey, FormFieldRequest request) {
        FormDefinition definition = getOwnedDefinition(formKey);
        validateFieldKey(request.fieldKey());
        if (fieldRepository.findByFormDefinitionIdAndFieldKey(definition.getId(), request.fieldKey()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Field key already exists for this form");
        }
        validateFieldDefinition(request.fieldType(), request.validationJson());
        String actor = currentActor();
        FormField field = new FormField();
        field.setFormDefinitionId(definition.getId());
        field.setFieldKey(request.fieldKey());
        field.setLabel(request.label().trim());
        field.setFieldType(request.fieldType());
        field.setRequired(request.required());
        field.setDisplayOrder(request.displayOrder());
        field.setPlaceholder(trimToNull(request.placeholder()));
        field.setHelpText(trimToNull(request.helpText()));
        field.setValidationJson(trimToNull(request.validationJson()));
        field.setCreatedBy(actor);
        field.setUpdatedBy(actor);
        field = fieldRepository.save(field);
        bumpVersion(definition, actor);
        return toFieldView(field, true);
    }

    @Transactional
    public FormFieldView updateField(UUID fieldId, FormFieldUpdateRequest request) {
        FormField field = getField(fieldId);
        if (field.isSystemManaged()
                && (field.getFieldType() != request.fieldType() || field.isRequired() != request.required())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System field type and requiredness are fixed");
        }
        validateFieldDefinition(request.fieldType(), request.validationJson());
        String actor = currentActor();
        field.setLabel(request.label().trim());
        field.setFieldType(request.fieldType());
        field.setRequired(request.required());
        field.setDisplayOrder(request.displayOrder());
        field.setPlaceholder(trimToNull(request.placeholder()));
        field.setHelpText(trimToNull(request.helpText()));
        field.setValidationJson(trimToNull(request.validationJson()));
        field.setUpdatedBy(actor);
        fieldRepository.save(field);
        bumpVersion(getDefinitionById(field.getFormDefinitionId()), actor);
        return toFieldView(field, true);
    }

    @Transactional
    public FormFieldView setFieldStatus(UUID fieldId, FormStatusRequest request) {
        FormField field = getField(fieldId);
        String actor = currentActor();
        field.setActive(request.active());
        field.setUpdatedBy(actor);
        fieldRepository.save(field);
        bumpVersion(getDefinitionById(field.getFormDefinitionId()), actor);
        return toFieldView(field, true);
    }

    @Transactional
    public List<FormFieldView> reorderFields(String formKey, List<UUID> orderedFieldIds) {
        FormDefinition definition = getOwnedDefinition(formKey);
        List<FormField> fields = fieldRepository.findByFormDefinitionIdOrderByDisplayOrderAsc(definition.getId());
        Set<UUID> activeIds = fields.stream().filter(FormField::isActive).map(FormField::getId).collect(Collectors.toSet());
        if (orderedFieldIds.size() != activeIds.size()
                || new HashSet<>(orderedFieldIds).size() != orderedFieldIds.size()
                || !new HashSet<>(orderedFieldIds).equals(activeIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder must include each active field exactly once");
        }
        Map<UUID, Integer> orders = new LinkedHashMap<>();
        for (int i = 0; i < orderedFieldIds.size(); i++) {
            orders.put(orderedFieldIds.get(i), i);
        }
        String actor = currentActor();
        fields.forEach(field -> {
            Integer order = orders.get(field.getId());
            if (order != null) {
                field.setDisplayOrder(order);
                field.setUpdatedBy(actor);
            }
        });
        fieldRepository.saveAll(fields);
        bumpVersion(definition, actor);
        return fields.stream()
                .sorted(Comparator.comparingInt(FormField::getDisplayOrder))
                .map(field -> toFieldView(field, true))
                .toList();
    }

    @Transactional
    public FormOptionView addOption(UUID fieldId, FormOptionRequest request) {
        FormField field = getField(fieldId);
        requireSelectField(field);
        List<FormFieldOption> existing = optionRepository.findByFieldIdOrderByDisplayOrderAsc(fieldId);
        if (existing.stream().anyMatch(option -> option.getValue().equals(request.value()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Option value already exists");
        }
        String actor = currentActor();
        FormFieldOption option = new FormFieldOption();
        option.setFieldId(fieldId);
        option.setValue(request.value());
        option.setLabel(request.label().trim());
        option.setDisplayOrder(request.displayOrder());
        option.setCreatedBy(actor);
        option.setUpdatedBy(actor);
        option = optionRepository.save(option);
        bumpVersion(getDefinitionById(field.getFormDefinitionId()), actor);
        return toOptionView(option);
    }

    @Transactional
    public FormOptionView updateOption(UUID optionId, FormOptionRequest request) {
        FormFieldOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found"));
        if (!option.getValue().equals(request.value())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stable option value cannot be changed");
        }
        String actor = currentActor();
        option.setLabel(request.label().trim());
        option.setDisplayOrder(request.displayOrder());
        option.setUpdatedBy(actor);
        optionRepository.save(option);
        FormField field = getField(option.getFieldId());
        bumpVersion(getDefinitionById(field.getFormDefinitionId()), actor);
        return toOptionView(option);
    }

    @Transactional
    public FormOptionView setOptionStatus(UUID optionId, FormStatusRequest request) {
        FormFieldOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found"));
        String actor = currentActor();
        option.setActive(request.active());
        option.setUpdatedBy(actor);
        optionRepository.save(option);
        FormField field = getField(option.getFieldId());
        bumpVersion(getDefinitionById(field.getFormDefinitionId()), actor);
        return toOptionView(option);
    }

    @Transactional
    public FormSubmissionView submit(FormSubmissionRequest request) {
        FormDefinition definition = getOwnedDefinition(request.formKey());
        if (!definition.isActive() || !managerMayUse(request.formKey()) && !isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Form submission is not permitted");
        }
        if (!request.recordType().equals(request.formKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record type must match the form's technical key");
        }
        UUID recordId = resolveRecordId(request.recordType(), request.recordId());
        FormRevision revision = revisionRepository
                .findByFormDefinitionIdAndVersion(definition.getId(), definition.getVersion())
                .orElseThrow(() -> new IllegalStateException("Current form revision is missing"));
        List<FormField> fields = fieldRepository.findByFormDefinitionIdOrderByDisplayOrderAsc(definition.getId())
                .stream().filter(FormField::isActive).toList();
        validateAnswers(fields, request.answers());
        String actor = currentActor();
        FormSubmission submission = new FormSubmission();
        submission.setFormDefinitionId(definition.getId());
        submission.setFormRevisionId(revision.getId());
        submission.setFormVersion(revision.getVersion());
        submission.setRecordType(request.recordType());
        submission.setRecordId(recordId);
        submission.setAnswersJson(writeJson(request.answers()));
        submission.setSubmittedBy(actor);
        submission = submissionRepository.save(submission);
        return toSubmissionView(submission, request.formKey(), revision);
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionView> getSubmissions(String recordType, UUID recordId) {
        if (!isAdmin() && !managerMayUse(recordType)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        return submissionRepository.findByRecordTypeAndRecordIdOrderBySubmittedAtDesc(recordType, recordId)
                .stream()
                .map(submission -> {
                    FormDefinition definition = getDefinitionById(submission.getFormDefinitionId());
                    FormRevision revision = revisionRepository.findById(submission.getFormRevisionId())
                            .orElseThrow(() -> new IllegalStateException("Submission revision is missing"));
                    return toSubmissionView(submission, definition.getFormKey(), revision);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public JsonNode getRevision(String formKey, int version) {
        FormDefinition definition = getOwnedDefinition(formKey);
        if (!isAdmin() && (!definition.isActive() || !managerMayUse(formKey))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found");
        }
        FormRevision revision = revisionRepository.findByFormDefinitionIdAndVersion(definition.getId(), version)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form revision not found"));
        return readJson(revision.getSnapshotJson());
    }

    private void validateAnswers(List<FormField> fields, Map<String, JsonNode> answers) {
        Map<String, FormField> activeFields = fields.stream()
                .collect(Collectors.toMap(FormField::getFieldKey, field -> field));
        if (!activeFields.keySet().containsAll(answers.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission contains an unknown or inactive field");
        }
        for (FormField field : fields) {
            JsonNode value = answers.get(field.getFieldKey());
            if (value == null || value.isNull()) {
                if (field.isRequired() && !field.isSystemManaged()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required field is missing: " + field.getFieldKey());
                }
                continue;
            }
            validateAnswer(field, value);
        }
    }

    private void validateAnswer(FormField field, JsonNode value) {
        switch (field.getFieldType()) {
            case TEXT, TEXTAREA, EMAIL, PHONE -> {
                if (!value.isTextual()) invalidValue(field);
                if (field.isRequired() && value.asText().isBlank()) invalidValue(field);
                if (field.getFieldType() == FieldType.EMAIL && !value.asText().isBlank() && !EMAIL_PATTERN.matcher(value.asText()).matches()) {
                    invalidValue(field);
                }
                if (field.getFieldType() == FieldType.PHONE && !value.asText().isBlank() && !PHONE_PATTERN.matcher(value.asText()).matches()) {
                    invalidValue(field);
                }
                int maxLength = field.getFieldType() == FieldType.TEXTAREA ? 10000 : 2000;
                if (value.asText().length() > maxLength) invalidValue(field);
                validateLengthConstraint(field, value.asText());
            }
            case FILE, IMAGE -> throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED, "Secure file upload is not enabled for this form");
            case NUMBER -> {
                if (!value.isIntegralNumber()) invalidValue(field);
                validateNumericConstraints(field, new BigDecimal(value.asText()));
            }
            case DECIMAL, CURRENCY, PERCENTAGE -> {
                if (!value.isNumber()) invalidValue(field);
                BigDecimal decimal = value.decimalValue();
                validateNumericConstraints(field, decimal);
                int scale = validationInteger(field, "scale", field.getFieldType() == FieldType.CURRENCY ? 2 : 6);
                if (decimal.scale() > scale) invalidValue(field);
                if (field.getFieldType() == FieldType.PERCENTAGE) {
                    if (decimal.compareTo(BigDecimal.ZERO) < 0 || decimal.compareTo(BigDecimal.valueOf(100)) > 0) {
                        invalidValue(field);
                    }
                }
            }
            case DATE -> {
                if (!value.isTextual()) invalidValue(field);
                try {
                    LocalDate.parse(value.asText());
                } catch (RuntimeException ex) {
                    invalidValue(field);
                }
            }
            case DATETIME -> {
                if (!value.isTextual()) invalidValue(field);
                try {
                    OffsetDateTime.parse(value.asText());
                } catch (RuntimeException ex) {
                    invalidValue(field);
                }
            }
            case BOOLEAN -> {
                if (!value.isBoolean()) invalidValue(field);
            }
            case SINGLE_SELECT, DROPDOWN -> {
                if (!value.isTextual() || !activeOptionValues(field.getId()).contains(value.asText())) invalidValue(field);
            }
            case MULTI_SELECT -> {
                if (!value.isArray()) invalidValue(field);
                if (field.isRequired() && value.isEmpty()) invalidValue(field);
                Set<String> allowed = activeOptionValues(field.getId());
                for (JsonNode item : value) {
                    if (!item.isTextual() || !allowed.contains(item.asText())) invalidValue(field);
                }
            }
        }
    }

    private void validateLengthConstraint(FormField field, String value) {
        int maxLength = validationInteger(field, "maxLength", field.getFieldType() == FieldType.TEXTAREA ? 10000 : 2000);
        if (value.length() > maxLength) invalidValue(field);
    }

    private void validateNumericConstraints(FormField field, BigDecimal value) {
        JsonNode constraints = readConstraints(field);
        if (constraints.has("min") && value.compareTo(constraints.get("min").decimalValue()) < 0) invalidValue(field);
        if (constraints.has("max") && value.compareTo(constraints.get("max").decimalValue()) > 0) invalidValue(field);
    }

    private int validationInteger(FormField field, String key, int fallback) {
        JsonNode constraints = readConstraints(field);
        if (!constraints.has(key)) return fallback;
        JsonNode value = constraints.get(key);
        if (!value.canConvertToInt() || value.asInt() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid field constraints");
        }
        return value.asInt();
    }

    private JsonNode readConstraints(FormField field) {
        if (field.getValidationJson() == null || field.getValidationJson().isBlank()) {
            return objectMapper.createObjectNode();
        }
        JsonNode constraints = readJson(field.getValidationJson());
        if (!constraints.isObject()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid field constraints");
        return constraints;
    }

    private void validateFieldDefinition(FieldType type, String validationJson) {
        if (validationJson == null || validationJson.isBlank()) return;
        JsonNode constraints = readJson(validationJson);
        if (!constraints.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation constraints must be a JSON object");
        }
        constraints.fieldNames().forEachRemaining(key -> {
            if (!VALIDATION_KEYS.contains(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported validation constraint: " + key);
            }
        });
        if (constraints.has("min") && !constraints.get("min").isNumber()
                || constraints.has("max") && !constraints.get("max").isNumber()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numeric bounds must be numbers");
        }
        if (constraints.has("min") && constraints.has("max")
                && constraints.get("min").decimalValue().compareTo(constraints.get("max").decimalValue()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum cannot exceed maximum");
        }
        if (constraints.has("maxLength") && (!constraints.get("maxLength").canConvertToInt()
                || constraints.get("maxLength").asInt() < 1 || constraints.get("maxLength").asInt() > 10000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxLength must be between 1 and 10000");
        }
        if (constraints.has("scale") && (!constraints.get("scale").canConvertToInt()
                || constraints.get("scale").asInt() < 0 || constraints.get("scale").asInt() > 8)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scale must be between 0 and 8");
        }
        if (type != FieldType.NUMBER && type != FieldType.DECIMAL && type != FieldType.CURRENCY && type != FieldType.PERCENTAGE
                && (constraints.has("min") || constraints.has("max") || constraints.has("scale"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numeric constraints require NUMBER, DECIMAL, CURRENCY or PERCENTAGE");
        }
        if ((type == FieldType.SINGLE_SELECT || type == FieldType.MULTI_SELECT || type == FieldType.DROPDOWN)
                && (constraints.has("min") || constraints.has("max") || constraints.has("scale") || constraints.has("maxLength"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select fields do not accept these constraints");
        }
    }

    private Set<String> activeOptionValues(UUID fieldId) {
        return optionRepository.findByFieldIdOrderByDisplayOrderAsc(fieldId).stream()
                .filter(FormFieldOption::isActive)
                .map(FormFieldOption::getValue)
                .collect(Collectors.toSet());
    }

    private void requireSelectField(FormField field) {
        if (field.getFieldType() != FieldType.SINGLE_SELECT && field.getFieldType() != FieldType.MULTI_SELECT && field.getFieldType() != FieldType.DROPDOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Options can only be added to select fields");
        }
    }

    private void bumpVersion(FormDefinition definition, String actor) {
        definition.setVersion(definition.getVersion() + 1);
        definition.setUpdatedBy(actor);
        definitionRepository.save(definition);
        saveRevision(definition, actor);
    }

    private void saveRevision(FormDefinition definition, String actor) {
        FormRevision revision = new FormRevision();
        revision.setFormDefinitionId(definition.getId());
        revision.setVersion(definition.getVersion());
        revision.setSnapshotJson(writeJson(toView(definition, true)));
        revision.setCreatedBy(actor);
        revisionRepository.save(revision);
    }

    private void createRevision(FormDefinition definition, String actor) {
        definition.setVersion(1);
        definitionRepository.save(definition);
        saveRevision(definition, actor);
    }

    private FormDefinitionView toView(FormDefinition definition, boolean includeInactive) {
        List<FormFieldView> fields = fieldRepository.findByFormDefinitionIdOrderByDisplayOrderAsc(definition.getId()).stream()
                .filter(field -> includeInactive || field.isActive())
                .map(field -> toFieldView(field, includeInactive))
                .toList();
        return new FormDefinitionView(
                definition.getId(), definition.getFormKey(), definition.getDisplayName(), definition.getDescription(),
                definition.isActive(), definition.getVersion(), definition.getCreatedAt(), definition.getUpdatedAt(),
                definition.getCreatedBy(), definition.getUpdatedBy(), fields);
    }

    private FormFieldView toFieldView(FormField field, boolean includeInactive) {
        List<FormOptionView> options = optionRepository.findByFieldIdOrderByDisplayOrderAsc(field.getId()).stream()
                .filter(option -> includeInactive || option.isActive())
                .map(this::toOptionView)
                .toList();
        return new FormFieldView(
                field.getId(), field.getFieldKey(), field.getLabel(), field.getFieldType(),
                field.isRequired(), field.isActive(), field.isSystemManaged(), field.getDisplayOrder(), field.getPlaceholder(),
                field.getHelpText(), field.getValidationJson(), options);
    }

    private FormOptionView toOptionView(FormFieldOption option) {
        return new FormOptionView(option.getId(), option.getValue(), option.getLabel(), option.getDisplayOrder(), option.isActive());
    }

    private FormSubmissionView toSubmissionView(FormSubmission submission, String formKey, FormRevision revision) {
        return new FormSubmissionView(
                submission.getId(), formKey, submission.getFormRevisionId(), submission.getFormVersion(),
                submission.getRecordType(), submission.getRecordId(), readJsonMap(submission.getAnswersJson()),
                submission.getSubmittedBy(), submission.getSubmittedAt(), readJson(revision.getSnapshotJson()));
    }

    private Map<String, JsonNode> readJsonMap(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, String.class, JsonNode.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored form submission JSON is invalid", ex);
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON value", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize form data", ex);
        }
    }

    private FormDefinition getOwnedDefinition(String formKey) {
        requireOwnedFormKey(formKey);
        return definitionRepository.findByFormKey(formKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
    }

    private FormDefinition getDefinitionById(UUID id) {
        FormDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Form definition is missing"));
        requireOwnedFormKey(definition.getFormKey());
        return definition;
    }

    private FormField getField(UUID id) {
        FormField field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));
        requireOwnedFormKey(getDefinitionById(field.getFormDefinitionId()).getFormKey());
        return field;
    }

    private void requireOwnedFormKey(String key) {
        validateFormKey(key);
    }

    public void assertActiveOption(String formKey, String fieldKey, String value) {
        FormDefinition definition = getOwnedDefinition(formKey);
        FormField field = fieldRepository.findByFormDefinitionIdAndFieldKey(definition.getId(), fieldKey)
                .filter(FormField::isActive)
                .orElse(null);
        if (field == null) {
            return;
        }
        requireSelectField(field);
        Set<String> activeOptions = activeOptionValues(field.getId());
        if (!activeOptions.isEmpty() && !activeOptions.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value is not an active form option");
        }
    }

    private UUID resolveRecordId(String recordType, UUID requestedRecordId) {
        FormRecordValidator validator = recordValidators.stream()
                .filter(candidate -> candidate.supports(recordType))
                .findFirst()
                .orElse(null);
        if (validator != null) {
            if (requestedRecordId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A linked core record ID is required");
            }
            validator.requireExistingRecord(recordType, requestedRecordId);
            return requestedRecordId;
        }
        if (CORE_FORM_KEYS.contains(recordType) || requestedRecordId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This form requires a domain-owned record adapter");
        }
        return UUID.randomUUID();
    }

    private void ensureInvestmentTypeField() {
        FormDefinition definition = definitionRepository.findByFormKey("INVESTMENT").orElseThrow();
        if (fieldRepository.findByFormDefinitionIdAndFieldKey(definition.getId(), "investmentType").isPresent()) {
            return;
        }
        FormField created = new FormField();
        created.setFormDefinitionId(definition.getId());
        created.setFieldKey("investmentType");
        created.setLabel("Investment Type");
        created.setFieldType(FieldType.SINGLE_SELECT);
        created.setRequired(true);
        created.setActive(true);
        created.setSystemManaged(false);
        created.setDisplayOrder(0);
        created.setCreatedBy("system");
        created.setUpdatedBy("system");
        fieldRepository.save(created);
        bumpVersion(definition, "system");
    }

    private void validateFormKey(String key) {
        if (!FORM_KEY_PATTERN.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid technical form key");
        }
    }

    private void validateFieldKey(String key) {
        if (!FIELD_KEY_PATTERN.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid technical field key");
        }
    }

    private boolean managerMayUse(String formKey) {
        return !MANAGER_RESTRICTED_FORMS.contains(formKey);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().equals("anonymousUser")) {
            throw new AccessDeniedException("Authenticated actor is required");
        }
        return auth.getName();
    }

    private String defaultDisplayName(String key) {
        return Arrays.stream(key.toLowerCase().split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void invalidValue(FormField field) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for field: " + field.getFieldKey());
    }
}
