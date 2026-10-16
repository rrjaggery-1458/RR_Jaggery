package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.RegistrationEmailOtp;
import com.rrjaggery.auth.entity.RegistrationEmailStatus;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.event.RegistrationEmailRequestedEvent;
import com.rrjaggery.auth.repository.RegistrationEmailOtpRepository;
import com.rrjaggery.auth.repository.UserRepository;
import com.rrjaggery.auth.dto.RegistrationEmailStatusResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PublicRegistrationService {

    private static final Duration OTP_LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofHours(1);
    private static final int MAX_SENDS_PER_WINDOW = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int OTP_LENGTH = 6;

    private final RegistrationEmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PublicRegistrationService(
            RegistrationEmailOtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            AuthService authService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.authService = authService;
    }

    public synchronized UUID requestRegistrationCode(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with this email already exists. Please sign in.");
        }

        Instant now = Instant.now();
        RegistrationEmailOtp challenge = otpRepository.findByEmail(email).orElseGet(RegistrationEmailOtp::new);
        if (challenge.getEmail() != null
                && challenge.getEmailDeliveryStatus() != RegistrationEmailStatus.FAILED
                && challenge.getLastSentAt() != null
                && challenge.getLastSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another code");
        }

        boolean newWindow = challenge.getSendWindowStartedAt() == null
                || challenge.getSendWindowStartedAt().plus(SEND_WINDOW).isBefore(now);
        int sendsInWindow = newWindow ? 0 : challenge.getSendCount();
        if (sendsInWindow >= MAX_SENDS_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification codes requested");
        }

        String code = createCode();
        challenge.setEmail(email);
        UUID challengeId = UUID.randomUUID();
        challenge.setChallengeToken(challengeId);
        challenge.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        challenge.setFullName(request.getFullName().trim());
        challenge.setPhone(request.getPhone());
        challenge.setBusinessName(request.getBusinessName());
        challenge.setGstin(request.getGstin());
        challenge.setOtpHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(now.plus(OTP_LIFETIME));
        challenge.setLastSentAt(now);
        challenge.setSendWindowStartedAt(newWindow ? now : challenge.getSendWindowStartedAt());
        challenge.setSendCount(sendsInWindow + 1);
        challenge.setAttempts(0);
        challenge.setEmailDeliveryStatus(RegistrationEmailStatus.PENDING);
        otpRepository.save(challenge);

        eventPublisher.publishEvent(new RegistrationEmailRequestedEvent(
                challengeId, email, code, (int) OTP_LIFETIME.toMinutes()));
        return challengeId;
    }

    @Transactional(readOnly = true)
    public RegistrationEmailStatusResponse getRegistrationEmailStatus(UUID challengeId) {
        RegistrationEmailOtp challenge = otpRepository.findByChallengeToken(challengeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Registration verification request was not found"));
        return new RegistrationEmailStatusResponse(challenge.getEmailDeliveryStatus());
    }

    public synchronized AuthResponse verifyRegistration(String emailInput, UUID challengeId, String code) {
        String email = normalizeEmail(emailInput);
        RegistrationEmailOtp challenge = otpRepository.findByEmailAndChallengeToken(email, challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        Instant now = Instant.now();
        if (!challenge.getExpiresAt().isAfter(now)) {
            otpRepository.delete(challenge);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        if (challenge.getAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            otpRepository.delete(challenge);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        if (!passwordEncoder.matches(code, challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
                otpRepository.delete(challenge);
            } else {
                otpRepository.save(challenge);
            }
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        if (userRepository.existsByEmail(email)) {
            otpRepository.delete(challenge);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        User user = new User(email, challenge.getPasswordHash(), challenge.getFullName(), challenge.getPhone(), CustomerType.RETAIL);
        user.setBusinessName(challenge.getBusinessName());
        user.setGstin(challenge.getGstin());
        user.setRoles(Set.of(Role.CUSTOMER));
        user.setEmailVerified(true);
        User saved = userRepository.save(user);
        otpRepository.delete(challenge);
        return authService.createAuthenticationResponse(saved);
    }

    private String createCode() {
        int value = secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", value);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}
