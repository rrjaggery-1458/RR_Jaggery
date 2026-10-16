package com.rrjaggery.auth.service;

import com.rrjaggery.auth.entity.RegistrationEmailOtp;
import com.rrjaggery.auth.entity.RegistrationEmailStatus;
import com.rrjaggery.auth.repository.RegistrationEmailOtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationEmailDeliveryStatusService {

    private final RegistrationEmailOtpRepository otpRepository;

    public RegistrationEmailDeliveryStatusService(RegistrationEmailOtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID challengeId, RegistrationEmailStatus status) {
        RegistrationEmailOtp challenge = otpRepository.findByChallengeToken(challengeId)
                .orElse(null);
        if (challenge == null) {
            return;
        }
        challenge.setEmailDeliveryStatus(status);
        otpRepository.save(challenge);
    }
}
