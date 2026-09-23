package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.AdminStatsDto;
import com.rrjaggery.auth.dto.CreateManagerRequest;
import com.rrjaggery.auth.dto.UpdateUserAdminRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createManager(CreateManagerRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email already exists: " + email);
        }

        User manager = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhone(),
                CustomerType.INTERNAL
        );
        manager.setRoles(Set.of(Role.MANAGER));
        manager.setEnabled(true);

        User saved = userRepository.save(manager);
        return UserDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserAdminRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBusinessName() != null) {
            user.setBusinessName(request.getBusinessName());
        }
        if (request.getGstin() != null) {
            user.setGstin(request.getGstin());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    @Transactional
    public UserDto toggleUserEnabled(UUID id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long adminCount = users.stream().filter(u -> u.getRoles().contains(Role.ADMIN)).count();
        long managerCount = users.stream().filter(u -> u.getRoles().contains(Role.MANAGER)).count();
        long retailCustomerCount = users.stream()
                .filter(u -> u.getCustomerType() == CustomerType.RETAIL && u.getRoles().contains(Role.CUSTOMER))
                .count();
        long wholesaleCustomerCount = users.stream()
                .filter(u -> u.getCustomerType() == CustomerType.REGISTERED_WHOLESALE)
                .count();
        long disabledCount = users.stream().filter(u -> !u.isEnabled()).count();

        return new AdminStatsDto(totalUsers, adminCount, managerCount, retailCustomerCount, wholesaleCustomerCount, disabledCount);
    }
}
