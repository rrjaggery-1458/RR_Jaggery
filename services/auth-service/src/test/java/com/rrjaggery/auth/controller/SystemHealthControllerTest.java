package com.rrjaggery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SystemHealthControllerTest {

    private static final String ADMIN_TEST_PASSWORD = "admin-test-password";
    private static final String MANAGER_TEST_PASSWORD = "manager-test-password";
    private static final String CUSTOMER_TEST_PASSWORD = "customer-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureTestAccounts() {
        ensureTestAccount("admin@rrjaggery.com", ADMIN_TEST_PASSWORD, Role.ADMIN);
        ensureTestAccount("manager@rrjaggery.com", MANAGER_TEST_PASSWORD, Role.MANAGER);
        ensureTestAccount("customer@rrjaggery.com", CUSTOMER_TEST_PASSWORD, Role.CUSTOMER);
    }

    private void ensureTestAccount(String email, String password, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User(
                    email,
                    passwordEncoder.encode(password),
                    role.name() + " Test User",
                    null,
                    CustomerType.INTERNAL
            );
            user.setRoles(Set.of(role));
            userRepository.save(user);
        }
    }

    private String getAuthToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    void anonymousUser_shouldBeDeniedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/system-health"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerUser_shouldBeForbidden() throws Exception {
        String token = getAuthToken("customer@rrjaggery.com", CUSTOMER_TEST_PASSWORD);
        mockMvc.perform(get("/api/v1/auth/admin/system-health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerUser_shouldBeForbidden() throws Exception {
        String token = getAuthToken("manager@rrjaggery.com", MANAGER_TEST_PASSWORD);
        mockMvc.perform(get("/api/v1/auth/admin/system-health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUser_shouldBeAllowedAndReturnHealthSnapshot() throws Exception {
        String token = getAuthToken("admin@rrjaggery.com", ADMIN_TEST_PASSWORD);
        mockMvc.perform(get("/api/v1/auth/admin/system-health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").exists())
                .andExpect(jsonPath("$.data.database").exists())
                .andExpect(jsonPath("$.data.services").isArray())
                .andExpect(jsonPath("$.data.schemas").isArray())
                .andExpect(jsonPath("$.data.containers").isArray())
                .andExpect(jsonPath("$.data.redis").exists())
                .andExpect(jsonPath("$.data.gateway").exists())
                .andExpect(jsonPath("$.data.runtime").exists())
                .andExpect(jsonPath("$.data.structureAndMigration").exists())
                .andExpect(jsonPath("$.data.backup").exists());
    }
}
