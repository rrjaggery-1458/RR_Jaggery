package com.rrjaggery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.entity.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void testAdminLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("admin@rrjaggery.com", "Admin@123");
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
    void testRegisterNewCustomer() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser" + System.currentTimeMillis() + "@example.com");
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
    }

    @Test
    void testManagerLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("manager@rrjaggery.com", "Manager@123");
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
