package com.rrjaggery.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Provides a safe no-op fallback JavaMailSender when no real SMTP configuration
 * is present (e.g. in tests or when SMTP_HOST is not set).
 *
 * In production, the real JavaMailSender bean configured via spring.mail.*
 * properties takes precedence and this fallback is never created.
 *
 * JavaMailSenderImpl#createMimeMessage() works without an SMTP host so
 * controllers and services can start up cleanly; actual mail delivery will
 * only be attempted when ContactInquiryService#verifyConfiguration() passes.
 */
@Configuration
public class MailFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}
