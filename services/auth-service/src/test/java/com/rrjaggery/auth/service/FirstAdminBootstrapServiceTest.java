package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FirstAdminBootstrapServiceTest {
    private static final String BOOTSTRAP_PASSWORD = "bootstrap-test-password-123";

    @Autowired
    private FirstAdminBootstrapService bootstrapService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void clearAccounts() {
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    void createsExactlyOneEncodedAdminWithAdminAuthorityAndNoOtherAccounts() {
        bootstrapService.bootstrap("first.admin@example.invalid", BOOTSTRAP_PASSWORD);

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
        User admin = users.get(0);
        assertEquals("first.admin@example.invalid", admin.getEmail());
        assertEquals(CustomerType.INTERNAL, admin.getCustomerType());
        assertEquals(Set.of(Role.ADMIN), admin.getRoles());
        assertTrue(admin.isEnabled());
        assertTrue(passwordEncoder.matches(BOOTSTRAP_PASSWORD, admin.getPasswordHash()));
        assertNotEquals(BOOTSTRAP_PASSWORD, admin.getPasswordHash());
        assertEquals(1, userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN));

        String token = authService.login(
                new LoginRequest("first.admin@example.invalid", BOOTSTRAP_PASSWORD)).getToken();
        assertEquals(List.of("ROLE_ADMIN"), tokenProvider.getRolesFromToken(token));
    }

    @Test
    void repeatedStartupDoesNotCreateOrModifyAnotherAdmin() {
        bootstrapService.bootstrap("first.admin@example.invalid", BOOTSTRAP_PASSWORD);
        User original = userRepository.findByEmail("first.admin@example.invalid").orElseThrow();
        String originalHash = original.getPasswordHash();

        bootstrapService.bootstrap("different.admin@example.invalid", "another-test-password-123");

        assertEquals(1, userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN));
        assertTrue(userRepository.findByEmail("first.admin@example.invalid").isPresent());
        assertFalse(userRepository.findByEmail("different.admin@example.invalid").isPresent());
        assertEquals(originalHash,
                userRepository.findByEmail("first.admin@example.invalid").orElseThrow().getPasswordHash());
    }

    @Test
    void concurrentBootstrapAttemptsCreateOnlyOneEnabledAdmin() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(
                    () -> bootstrapService.bootstrap("first.admin@example.invalid", BOOTSTRAP_PASSWORD));
            Future<?> second = executor.submit(
                    () -> bootstrapService.bootstrap("second.admin@example.invalid", BOOTSTRAP_PASSWORD));

            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN));
        assertEquals(1, userRepository.count());
    }

    @Test
    void enabledAdminCausesBootstrapToSkipEvenWhenCredentialsAreMissing() {
        User existingAdmin = new User(
                "existing.admin@example.invalid",
                passwordEncoder.encode("existing-admin-password"),
                "Existing Admin",
                null,
                CustomerType.INTERNAL);
        existingAdmin.setRoles(Set.of(Role.ADMIN));
        userRepository.saveAndFlush(existingAdmin);
        String originalHash = existingAdmin.getPasswordHash();

        bootstrapService.bootstrap(null, null);

        User retained = userRepository.findByEmail("existing.admin@example.invalid").orElseThrow();
        assertEquals(1, userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN));
        assertEquals(originalHash, retained.getPasswordHash());
        assertEquals("Existing Admin", retained.getFullName());
    }

    @Test
    void disabledHistoricalAdminIsNotReactivatedAndDoesNotBlockOneNewEnabledAdmin() {
        User disabledAdmin = new User(
                "disabled.admin@example.invalid",
                passwordEncoder.encode("disabled-admin-password"),
                "Disabled Admin",
                null,
                CustomerType.INTERNAL);
        disabledAdmin.setRoles(Set.of(Role.ADMIN));
        disabledAdmin.setEnabled(false);
        userRepository.saveAndFlush(disabledAdmin);

        bootstrapService.bootstrap("new.admin@example.invalid", BOOTSTRAP_PASSWORD);

        assertEquals(1, userRepository.countByEnabledTrueAndRolesContaining(Role.ADMIN));
        assertFalse(userRepository.findByEmail("disabled.admin@example.invalid").orElseThrow().isEnabled());
        assertTrue(userRepository.findByEmail("new.admin@example.invalid").orElseThrow().isEnabled());
    }

    @Test
    void missingCredentialsFailClosedWithoutCreatingAnAccount() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.bootstrap(" ", null));

        assertTrue(exception.getMessage().contains("BOOTSTRAP_ADMIN_USERNAME"));
        assertFalse(exception.getMessage().contains(BOOTSTRAP_PASSWORD));
        assertEquals(0, userRepository.count());
    }

    @Test
    void bootstrapDoesNotCreateManagerOrCustomerAccounts() {
        bootstrapService.bootstrap("first.admin@example.invalid", BOOTSTRAP_PASSWORD);

        assertEquals(0, userRepository.countByEnabledTrueAndRolesContaining(Role.MANAGER));
        assertEquals(0, userRepository.countByEnabledTrueAndRolesContaining(Role.CUSTOMER));
        assertEquals(1, userRepository.count());
    }
}
