package com.rrjaggery.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:" + SecurityConstants.DEFAULT_JWT_SECRET + "}") String secret,
            @Value("${app.jwt.expiration-ms:" + SecurityConstants.DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, List<String> roles, String customerType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim(SecurityConstants.CLAIM_EMAIL, email)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_CUSTOMER_TYPE, customerType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_ROLES, List.class);
    }
}
