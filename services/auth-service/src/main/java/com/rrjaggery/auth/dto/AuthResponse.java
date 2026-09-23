package com.rrjaggery.auth.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private UserDto user;

    public AuthResponse() {}

    public AuthResponse(String token, long expiresInMs, UserDto user) {
        this.token = token;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInMs() { return expiresInMs; }
    public UserDto getUser() { return user; }
}
