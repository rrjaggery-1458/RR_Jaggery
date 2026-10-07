package com.rrjaggery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.auth.dto.CreateManagerRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    private static final String ADMIN_TEST_PASSWORD = "admin-test-password";
    private static final String MANAGER_TEST_PASSWORD = "manager-test-password";

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

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void testAdminLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("admin@rrjaggery.com", ADMIN_TEST_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("admin@rrjaggery.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.roles.length()").value(1));
    }

    @Test
    void testInvalidLoginFailure() throws Exception {
        LoginRequest req = new LoginRequest("admin@rrjaggery.com", "WrongPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testMalformedLoginRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{email:}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterNewCustomer() throws Exception {
        RegisterRequest req = new RegisterRequest();
        String email = "newuser" + System.currentTimeMillis() + "@example.com";
        req.setEmail(email);
        req.setPassword("Password@123");
        req.setFullName("Test User");
        req.setPhone("+91 99999 88888");
        req.setCustomerType(CustomerType.RETAIL);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists());

        User registered = userRepository.findByEmail(email).orElseThrow();
        assertEquals(Set.of(Role.CUSTOMER), registered.getRoles());
    }

    @Test
    void managerCannotCreateAnotherManager() throws Exception {
        LoginRequest login = new LoginRequest("manager@rrjaggery.com", MANAGER_TEST_PASSWORD);
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String managerToken = objectMapper.readTree(loginBody).path("data").path("token").asText();
        CreateManagerRequest request = new CreateManagerRequest(
                "unauthorized.manager@example.com",
                "manager-password",
                "Unauthorized Manager",
                null);

        mockMvc.perform(post("/api/v1/auth/admin/managers")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        assertTrue(userRepository.findByEmail("unauthorized.manager@example.com").isEmpty());
    }

    @Test
    void adminCanCreateManager() throws Exception {
        LoginRequest login = new LoginRequest("admin@rrjaggery.com", ADMIN_TEST_PASSWORD);
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String adminToken = objectMapper.readTree(loginBody).path("data").path("token").asText();
        CreateManagerRequest request = new CreateManagerRequest(
                "created.manager@example.com",
                "manager-password",
                "Created Manager",
                null);

        mockMvc.perform(post("/api/v1/auth/admin/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User created = userRepository.findByEmail("created.manager@example.com").orElseThrow();
        assertEquals(Set.of(Role.MANAGER), created.getRoles());
    }

    @Test
    void testManagerLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("manager@rrjaggery.com", MANAGER_TEST_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("manager@rrjaggery.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("MANAGER"));
    }

    @Test
    void testWholesaleLoginDisabled() throws Exception {
        LoginRequest req = new LoginRequest("wholesale@example.com", "Wholesale@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
