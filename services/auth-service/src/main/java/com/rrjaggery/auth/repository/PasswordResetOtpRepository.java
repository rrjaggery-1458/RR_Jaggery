package com.rrjaggery.auth.repository;

import com.rrjaggery.auth.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {
    Optional<PasswordResetOtp> findByEmail(String email);
    Optional<PasswordResetOtp> findByChallengeToken(UUID challengeToken);
    /** Clean up expired rows (called by scheduled task). */
    void deleteByExpiresAtBefore(Instant threshold);
}
