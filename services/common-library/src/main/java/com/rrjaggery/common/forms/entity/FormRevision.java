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
@Table(name = "form_revisions", indexes = {
        @Index(name = "uq_form_revision", columnList = "form_definition_id,version", unique = true)
})
public class FormRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_definition_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_form_revision_definition"))
    private FormDefinition formDefinition;

    @Column(nullable = false)
    private int version;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    public UUID getId() { return id; }
    public UUID getFormDefinitionId() { return formDefinitionId; }
    public void setFormDefinitionId(UUID formDefinitionId) { this.formDefinitionId = formDefinitionId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
