package com.rrjaggery.common.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_PRODUCTION_MANAGER = "ROLE_PRODUCTION_MANAGER";
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_CUSTOMER_TYPE = "customerType";

    public static final String DEFAULT_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    public static final long DEFAULT_EXPIRATION_MS = 86400000L; // 24 hours
}
