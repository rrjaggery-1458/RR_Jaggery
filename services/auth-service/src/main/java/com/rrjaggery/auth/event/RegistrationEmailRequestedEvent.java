package com.rrjaggery.auth.event;

import java.util.UUID;

public record RegistrationEmailRequestedEvent(
        UUID challengeId,
        String email,
        String code,
        int expiryMinutes) {
}
