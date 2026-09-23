package com.rrjaggery.auth.config;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    public void run(String... args) {
        // Seed default Admin
        if (!userRepository.existsByEmail("admin@rrjaggery.com")) {
            User admin = new User(
                    "admin@rrjaggery.com",
                    passwordEncoder.encode("Admin@123"),
                    "RR Master Admin",
                    "+91 98765 43210",
                    CustomerType.INTERNAL
            );
            admin.setRoles(Set.of(Role.ADMIN, Role.CUSTOMER));
            userRepository.save(admin);
            System.out.println("Seeded Default Admin: admin@rrjaggery.com / Admin@123");
        }

        // Seed demo Retail Customer
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

        // Seed demo Wholesale Customer
        if (!userRepository.existsByEmail("wholesale@example.com")) {
            User wholesale = new User(
                    "wholesale@example.com",
                    passwordEncoder.encode("Wholesale@123"),
                    "Karnataka Sweet Mart",
                    "+91 98450 12345",
                    CustomerType.REGISTERED_WHOLESALE
            );
            wholesale.setBusinessName("Karnataka Sweet Mart Pvt Ltd");
            wholesale.setGstin("29AAAAA0000A1Z5");
            wholesale.setRoles(Set.of(Role.CUSTOMER));
            userRepository.save(wholesale);
            System.out.println("Seeded Demo Wholesale Customer: wholesale@example.com / Wholesale@123");
        }
    }
}
