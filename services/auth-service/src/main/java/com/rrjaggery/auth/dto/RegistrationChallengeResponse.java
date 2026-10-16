package com.rrjaggery.auth.dto;

import java.util.UUID;

public class RegistrationChallengeResponse {

    private final UUID challengeId;

    public RegistrationChallengeResponse(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public UUID getChallengeId() { return challengeId; }
}
