package com.rrjaggery.auth.service;

import com.rrjaggery.auth.entity.RegistrationEmailStatus;
import com.rrjaggery.auth.event.RegistrationEmailRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationEmailDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(RegistrationEmailDeliveryListener.class);

    private final RegistrationEmailSender emailSender;
    private final RegistrationEmailDeliveryStatusService statusService;

    public RegistrationEmailDeliveryListener(
            RegistrationEmailSender emailSender,
            RegistrationEmailDeliveryStatusService statusService) {
        this.emailSender = emailSender;
        this.statusService = statusService;
    }

    @Async("registrationEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendRegistrationCode(RegistrationEmailRequestedEvent event) {
        try {
            emailSender.sendRegistrationCode(event.email(), event.code(), event.expiryMinutes());
            statusService.updateStatus(event.challengeId(), RegistrationEmailStatus.SENT);
        } catch (RuntimeException exception) {
            log.warn("Failed to send registration verification email for challenge {}", event.challengeId(), exception);
            statusService.updateStatus(event.challengeId(), RegistrationEmailStatus.FAILED);
        }
    }
}
