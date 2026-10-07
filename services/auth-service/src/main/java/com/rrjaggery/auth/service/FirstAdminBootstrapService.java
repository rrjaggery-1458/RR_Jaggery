package com.rrjaggery.auth.service;

import com.rrjaggery.auth.config.BootstrapAdminLock;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FirstAdminBootstrapService {
    private static final Logger log = LoggerFactory.getLogger(FirstAdminBootstrapService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminLock bootstrapAdminLock;

    public FirstAdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BootstrapAdminLock bootstrapAdminLock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminLock = bootstrapAdminLock;
    }

    @Transactional
    public void bootstrap(String configuredUsername, String configuredPassword) {
        bootstrapAdminLock.acquire();
        if (userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN) > 0) {
            log.info("First-Admin bootstrap skipped because an enabled ADMIN account already exists.");
            return;
        }

        String username = configuredUsername == null ? "" : configuredUsername.trim();
        if (username.isEmpty() || configuredPassword == null || configuredPassword.isBlank()) {
            throw new IllegalStateException(
                    "No enabled ADMIN account exists. Configure BOOTSTRAP_ADMIN_USERNAME and "
                            + "BOOTSTRAP_ADMIN_PASSWORD before starting auth-service.");
        }
        if (username.length() > 150 || !EMAIL_PATTERN.matcher(username).matches()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME must be a valid email address.");
        }
        if (configuredPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters.");
        }

        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        User user = new User(
                normalizedUsername,
                passwordEncoder.encode(configuredPassword),
                normalizedUsername,
                null,
                CustomerType.INTERNAL);
        user.setRoles(Set.of(Role.ADMIN));
        user.setEnabled(true);
        userRepository.saveAndFlush(user);
        log.info("Created the first ADMIN account from configured bootstrap credentials.");
    }
}
