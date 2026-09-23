package com.rrjaggery.auth.config;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed / enforce default Admin
        if (!userRepository.existsByEmail("admin@rrjaggery.com")) {
            User admin = new User(
                    "admin@rrjaggery.com",
                    passwordEncoder.encode("Admin@123"),
                    "RR Master Admin",
                    "+91 98765 43210",
                    CustomerType.INTERNAL
            );
            admin.setRoles(Set.of(Role.ADMIN));
            userRepository.save(admin);
            System.out.println("Seeded Default Admin: admin@rrjaggery.com / Admin@123 (Role: ADMIN only)");
        } else {
            // Ensure admin has Role.ADMIN ONLY
            userRepository.findByEmail("admin@rrjaggery.com").ifPresent(admin -> {
                if (!admin.getRoles().equals(Set.of(Role.ADMIN))) {
                    admin.setRoles(Set.of(Role.ADMIN));
                    userRepository.save(admin);
                    System.out.println("Enforced single authoritative Role.ADMIN for admin@rrjaggery.com");
                }
            });
        }

        // 2. Seed default Manager
        if (!userRepository.existsByEmail("manager@rrjaggery.com")) {
            User manager = new User(
                    "manager@rrjaggery.com",
                    passwordEncoder.encode("Manager@123"),
                    "RR Operations Manager",
                    "+91 98765 43211",
                    CustomerType.INTERNAL
            );
            manager.setRoles(Set.of(Role.MANAGER));
            manager.setEnabled(true);
            userRepository.save(manager);
            System.out.println("Seeded Default Manager: manager@rrjaggery.com / Manager@123");
        }

        // 3. Seed demo Retail Customer
        if (!userRepository.existsByEmail("retail@example.com")) {
            User retail = new User(
                    "retail@example.com",
                    passwordEncoder.encode("Retail@123"),
                    "Rohan Sharma",
                    "+91 91234 56789",
                    CustomerType.RETAIL
            );
            retail.setRoles(Set.of(Role.CUSTOMER));
            userRepository.save(retail);
            System.out.println("Seeded Demo Retail Customer: retail@example.com / Retail@123");
        }

        // 4. Migrate wholesale login accounts (disable login, keep customer_type REGISTERED_WHOLESALE)
        userRepository.findAll().forEach(user -> {
            boolean updated = false;

            // Disable wholesale portal login
            if (user.getCustomerType() == CustomerType.REGISTERED_WHOLESALE && user.isEnabled()) {
                user.setEnabled(false);
                updated = true;
                System.out.println("Disabled wholesale portal account for: " + user.getEmail());
            }

            // Migrate legacy PRODUCTION_MANAGER role to MANAGER
            if (user.getRoles().contains(Role.PRODUCTION_MANAGER)) {
                Set<Role> updatedRoles = new HashSet<>(user.getRoles());
                updatedRoles.remove(Role.PRODUCTION_MANAGER);
                updatedRoles.add(Role.MANAGER);
                user.setRoles(updatedRoles);
                updated = true;
                System.out.println("Migrated PRODUCTION_MANAGER to MANAGER for user: " + user.getEmail());
            }

            // Disable and remove legacy EMPLOYEE role
            if (user.getRoles().contains(Role.EMPLOYEE)) {
                Set<Role> updatedRoles = new HashSet<>(user.getRoles());
                updatedRoles.remove(Role.EMPLOYEE);
                user.setRoles(updatedRoles);
                user.setEnabled(false);
                updated = true;
                System.out.println("Disabled and removed EMPLOYEE role for user: " + user.getEmail());
            }

            if (updated) {
                userRepository.save(user);
            }
        });
    }
}
