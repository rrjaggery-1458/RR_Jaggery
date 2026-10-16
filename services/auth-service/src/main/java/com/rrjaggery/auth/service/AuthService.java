package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import com.rrjaggery.common.security.JwtTokenProvider;
import com.rrjaggery.common.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long expirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${app.jwt.expiration-ms:" + SecurityConstants.DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.expirationMs = expirationMs;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is disabled. Please contact support.");
        }

        boolean internalAccount = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.MANAGER);
        if (!internalAccount && !user.isEmailVerified()) {
            throw new IllegalStateException("Email address is not verified. Complete email verification before logging in.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return createAuthenticationResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return UserDto.fromEntity(user);
    }

    AuthResponse createAuthenticationResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> "ROLE_" + r.name())
                .collect(Collectors.toList());

        String token = tokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                roleNames,
                user.getCustomerType() != null ? user.getCustomerType().name() : CustomerType.RETAIL.name()
        );

        return new AuthResponse(token, expirationMs, UserDto.fromEntity(user));
    }
}
