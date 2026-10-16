package com.rrjaggery.contact.service;

import com.rrjaggery.contact.dto.ContactInquiryRequest;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class ContactInquiryService {

    private static final Logger log = LoggerFactory.getLogger(ContactInquiryService.class);

    private final JavaMailSender mailSender;
    private final String smtpHost;
    private final boolean smtpAuth;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String senderAddress;
    private final String recipientAddress;

    public ContactInquiryService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword,
            @Value("${app.contact.from:}") String senderAddress,
            @Value("${app.contact.recipient:}") String recipientAddress) {
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.smtpAuth = smtpAuth;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
    }

    public void sendInquiry(ContactInquiryRequest inquiry) {
        verifyConfiguration();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress);
            helper.setTo(recipientAddress);
            helper.setReplyTo(inquiry.getEmail());
            helper.setSubject("Website contact form inquiry");
            helper.setText(String.format(
                    "Name: %s%nEmail: %s%nPhone: %s%n%nMessage:%n%s",
                    inquiry.getName(),
                    inquiry.getEmail(),
                    inquiry.getPhone(),
                    inquiry.getMessage()
            ), false);
            mailSender.send(message);
        } catch (AddressException ex) {
            log.error("Contact email address configuration is invalid", ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured with valid sender and recipient addresses."
            );
        } catch (MailException ex) {
            log.error("SMTP delivery failed for public contact inquiry", ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email delivery failed because the configured SMTP service could not accept the message. Please try again later."
            );
        } catch (MessagingException ex) {
            log.error("Could not construct public contact inquiry email", ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service failed while preparing the inquiry. Please try again later."
            );
        }
    }

    private void verifyConfiguration() {
        if (smtpHost == null || smtpHost.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured: set SMTP_HOST and SMTP credentials."
            );
        }
        if (smtpAuth && (smtpUsername == null || smtpUsername.isBlank()
                || smtpPassword == null || smtpPassword.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured: SMTP_AUTH is enabled, so set SMTP_USERNAME and SMTP_PASSWORD."
            );
        }
        if (senderAddress == null || senderAddress.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured: set CONTACT_EMAIL_FROM or SMTP_USERNAME."
            );
        }
        if (recipientAddress == null || recipientAddress.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured: set CONTACT_EMAIL_RECIPIENT."
            );
        }
        validateConfiguredAddress(senderAddress);
        validateConfiguredAddress(recipientAddress);
    }

    private void validateConfiguredAddress(String address) {
        try {
            new InternetAddress(address, true).validate();
        } catch (AddressException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contact email service is not configured with valid sender and recipient addresses."
            );
        }
    }
}
