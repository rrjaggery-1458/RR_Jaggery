package com.rrjaggery.common.forms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_submissions", indexes = {
        @Index(name = "idx_form_submission_record", columnList = "record_type,record_id"),
        @Index(name = "idx_form_submission_definition", columnList = "form_definition_id")
})
public class FormSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_definition_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_form_submission_definition"))
    private FormDefinition formDefinition;

    @Column(name = "form_revision_id", nullable = false)
    private UUID formRevisionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_revision_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_form_submission_revision"))
    private FormRevision formRevision;

    @Column(name = "form_version", nullable = false)
    private int formVersion;

    @Column(name = "record_type", nullable = false, length = 80)
    private String recordType;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "answers_json", nullable = false, columnDefinition = "text")
    private String answersJson;

    @Column(name = "submitted_by", nullable = false, length = 255)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getFormDefinitionId() { return formDefinitionId; }
    public void setFormDefinitionId(UUID formDefinitionId) { this.formDefinitionId = formDefinitionId; }
    public UUID getFormRevisionId() { return formRevisionId; }
    public void setFormRevisionId(UUID formRevisionId) { this.formRevisionId = formRevisionId; }
    public int getFormVersion() { return formVersion; }
    public void setFormVersion(int formVersion) { this.formVersion = formVersion; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
}
