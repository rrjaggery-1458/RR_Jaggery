package com.rrjaggery.auth.repository;

import com.rrjaggery.auth.entity.RegistrationEmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationEmailOtpRepository extends JpaRepository<RegistrationEmailOtp, UUID> {
    Optional<RegistrationEmailOtp> findByEmail(String email);
    Optional<RegistrationEmailOtp> findByChallengeToken(UUID challengeToken);
    Optional<RegistrationEmailOtp> findByEmailAndChallengeToken(String email, UUID challengeToken);
}
