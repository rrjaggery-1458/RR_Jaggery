package com.rrjaggery.auth.dto;

import com.rrjaggery.auth.entity.RegistrationEmailStatus;

public class RegistrationEmailStatusResponse {

    private final RegistrationEmailStatus status;

    public RegistrationEmailStatusResponse(RegistrationEmailStatus status) {
        this.status = status;
    }

    public RegistrationEmailStatus getStatus() {
        return status;
    }
}
