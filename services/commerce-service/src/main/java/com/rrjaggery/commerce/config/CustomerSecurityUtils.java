package com.rrjaggery.commerce.config;

import com.rrjaggery.common.security.JwtTokenProvider;
import com.rrjaggery.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Component
public class CustomerSecurityUtils {

    private final JwtTokenProvider tokenProvider;

    public CustomerSecurityUtils(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public static class AuthenticatedCustomer {
        private final UUID userId;
        private final String email;
        private final String customerType;
        private final boolean isAdmin;

        public AuthenticatedCustomer(UUID userId, String email, String customerType, boolean isAdmin) {
            this.userId = userId;
            this.email = email;
            this.customerType = customerType;
            this.isAdmin = isAdmin;
        }

        public UUID getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getCustomerType() { return customerType; }
        public boolean isAdmin() { return isAdmin; }
    }

    public AuthenticatedCustomer getAuthenticatedCustomer(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            String token = bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
            if (tokenProvider.validateToken(token)) {
                Claims claims = tokenProvider.getClaims(token);
                String sub = claims.getSubject();
                UUID userId = null;
                try {
                    userId = UUID.fromString(sub);
                } catch (Exception e) {
                    // Fallback to deterministic UUID based on email/subject
                    userId = UUID.nameUUIDFromBytes(sub.getBytes());
                }

                String email = claims.get(SecurityConstants.CLAIM_EMAIL, String.class);
                if (email == null) email = sub;

                String customerType = claims.get(SecurityConstants.CLAIM_CUSTOMER_TYPE, String.class);
                if (customerType == null) customerType = "RETAIL";

                List<String> roles = tokenProvider.getRolesFromToken(token);
                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r.contains("ADMIN"));

                return new AuthenticatedCustomer(userId, email, customerType, isAdmin);
            }
        }

        // Check if Spring SecurityContext has an authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String name = auth.getName();
            UUID userId = UUID.nameUUIDFromBytes(name.getBytes());
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.contains("ADMIN"));
            return new AuthenticatedCustomer(userId, name, "RETAIL", isAdmin);
        }

        throw new SecurityException("Full authentication is required to access this resource.");
    }
}
