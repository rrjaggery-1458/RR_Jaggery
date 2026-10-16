package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.ForgotPasswordChallengeResponse;
import com.rrjaggery.auth.dto.ResetPasswordRequest;
import com.rrjaggery.auth.entity.PasswordResetOtp;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.PasswordResetOtpRepository;
import com.rrjaggery.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final RegistrationEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            RegistrationEmailSender emailSender,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ForgotPasswordChallengeResponse requestPasswordReset(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Check if email is registered in the system
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "This email is not registered with RR Jaggery. Please check your email address or create a new account."));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive. Please contact support.");
        }

        // 2. Remove any previous reset request for this email
        otpRepository.findByEmail(normalizedEmail).ifPresent(otpRepository::delete);

        // 3. Generate 6-digit OTP
        int randomPin = 100_000 + secureRandom.nextInt(900_000);
        String otpCode = String.valueOf(randomPin);

        // 4. Save OTP record
        PasswordResetOtp resetOtp = new PasswordResetOtp();
        UUID challengeToken = UUID.randomUUID();
        resetOtp.setChallengeToken(challengeToken);
        resetOtp.setEmail(normalizedEmail);
        resetOtp.setOtpHash(passwordEncoder.encode(otpCode));
        resetOtp.setExpiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        resetOtp.setAttempts(0);
        resetOtp.setVerified(false);

        otpRepository.save(resetOtp);

        // 5. Send branded OTP email
        try {
            emailSender.sendPasswordResetOtp(user.getEmail(), user.getFullName(), otpCode, OTP_EXPIRY_MINUTES);
            log.info("Password reset OTP sent to registered email: {}", normalizedEmail);
        } catch (Exception ex) {
            log.error("Failed to deliver password reset OTP email to {}: {}", normalizedEmail, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to send reset code to your email. Please try again later.", ex);
        }

        return new ForgotPasswordChallengeResponse(
                challengeToken,
                "A 6-digit password reset OTP has been sent to " + normalizedEmail + ".");
    }

    @Transactional
    public boolean verifyOtp(UUID challengeToken, String otp) {
        if (challengeToken == null || !StringUtils.hasText(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge token and OTP are required");
        }

        PasswordResetOtp resetOtp = otpRepository.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired reset session. Please request a new OTP."));

        if (resetOtp.getExpiresAt().isBefore(Instant.now())) {
            otpRepository.delete(resetOtp);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired. Please request a new OTP.");
        }

        if (resetOtp.getAttempts() >= MAX_ATTEMPTS) {
            otpRepository.delete(resetOtp);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(otp.trim(), resetOtp.getOtpHash())) {
            resetOtp.setAttempts(resetOtp.getAttempts() + 1);
            otpRepository.save(resetOtp);
            int remaining = MAX_ATTEMPTS - resetOtp.getAttempts();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        resetOtp.setVerified(true);
        otpRepository.save(resetOtp);
        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getChallengeToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge token is required");
        }
        if (!StringUtils.hasText(request.getNewPassword()) || request.getNewPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }

        PasswordResetOtp resetOtp = otpRepository.findByChallengeToken(request.getChallengeToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired reset session. Please request a new OTP."));

        if (resetOtp.getExpiresAt().isBefore(Instant.now())) {
            otpRepository.delete(resetOtp);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset session has expired. Please request a new OTP.");
        }

        // If not already marked verified, check OTP now
        if (!resetOtp.isVerified()) {
            if (!StringUtils.hasText(request.getOtp())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required to reset password");
            }
            if (resetOtp.getAttempts() >= MAX_ATTEMPTS) {
                otpRepository.delete(resetOtp);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many incorrect attempts. Please request a new OTP.");
            }
            if (!passwordEncoder.matches(request.getOtp().trim(), resetOtp.getOtpHash())) {
                resetOtp.setAttempts(resetOtp.getAttempts() + 1);
                otpRepository.save(resetOtp);
                int remaining = MAX_ATTEMPTS - resetOtp.getAttempts();
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid OTP. " + remaining + " attempt(s) remaining.");
            }
        }

        // Look up the user
        User user = userRepository.findByEmail(resetOtp.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found."));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate OTP record so it cannot be reused
        otpRepository.delete(resetOtp);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
