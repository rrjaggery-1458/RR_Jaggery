package com.rrjaggery.auth.config;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Order(0)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Retain only legacy-account migration; account creation is an explicit operation.
        userRepository.findAll().forEach(user -> {
            boolean updated = false;

            // Wholesale records do not receive portal logins.
            if (user.getCustomerType() == CustomerType.REGISTERED_WHOLESALE && user.isEnabled()) {
                user.setEnabled(false);
                updated = true;
            }

            if (user.getRoles().contains(Role.PRODUCTION_MANAGER)) {
                Set<Role> updatedRoles = new HashSet<>(user.getRoles());
                updatedRoles.remove(Role.PRODUCTION_MANAGER);
                updatedRoles.add(Role.MANAGER);
                user.setRoles(updatedRoles);
                updated = true;
            }

            if (user.getRoles().contains(Role.EMPLOYEE)) {
                Set<Role> updatedRoles = new HashSet<>(user.getRoles());
                updatedRoles.remove(Role.EMPLOYEE);
                user.setRoles(updatedRoles);
                user.setEnabled(false);
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
            }
        });
    }
}
