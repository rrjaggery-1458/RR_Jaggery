package com.rrjaggery.auth.dto;

import java.util.UUID;

public class ForgotPasswordChallengeResponse {
    private final UUID challengeToken;
    private final String message;

    public ForgotPasswordChallengeResponse(UUID challengeToken, String message) {
        this.challengeToken = challengeToken;
        this.message = message;
    }

    public UUID getChallengeToken() { return challengeToken; }
    public String getMessage() { return message; }
}
