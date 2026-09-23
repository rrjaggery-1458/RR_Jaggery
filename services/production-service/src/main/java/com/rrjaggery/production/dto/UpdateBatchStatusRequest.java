package com.rrjaggery.production.dto;

import com.rrjaggery.production.entity.BatchStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateBatchStatusRequest {

    @NotNull(message = "Status is required")
    private BatchStatus status;

    private String actor;
    private String notes;

    public UpdateBatchStatusRequest() {
    }

    public UpdateBatchStatusRequest(BatchStatus status, String actor, String notes) {
        this.status = status;
        this.actor = actor;
        this.notes = notes;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
