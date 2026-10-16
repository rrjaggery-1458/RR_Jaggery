package com.rrjaggery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.auth.dto.CreateManagerRequest;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.mail.from=auth-test@example.com")
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

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private JavaMailSender mailSender;

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
        req.setCustomerType(CustomerType.REGISTERED_WHOLESALE);

        String registrationStart = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.challengeId").exists())
                .andReturn().getResponse().getContentAsString();
        assertTrue(userRepository.findByEmail(email).isEmpty(), "Account must not exist before email OTP verification");
        String challengeId = objectMapper.readTree(registrationStart).path("data").path("challengeId").asText();
        awaitRegistrationEmailStatus(challengeId, "SENT");

        ArgumentCaptor<SimpleMailMessage> emailMessage = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailMessage.capture());
        Matcher codeMatcher = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)")
                .matcher(emailMessage.getValue().getText());
        assertTrue(codeMatcher.find(), "Email should contain the six-digit verification code");
        String verifyJson = objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "challengeId", challengeId, "otp", codeMatcher.group()));
        mockMvc.perform(post("/api/v1/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").exists());

        User registered = userRepository.findByEmail(email).orElseThrow();
        assertEquals(Set.of(Role.CUSTOMER), registered.getRoles());
        assertEquals(CustomerType.RETAIL, registered.getCustomerType(),
                "Public registration must ignore a submitted customer type");
        assertTrue(registered.isEmailVerified());
    }

    @Test
    void testIncorrectRegistrationOtpDoesNotCreateAccount() throws Exception {
        String email = "wrongotp" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = registrationRequest(email);
        String registrationStart = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String challengeId = objectMapper.readTree(registrationStart).path("data").path("challengeId").asText();
        awaitRegistrationEmailStatus(challengeId, "SENT");
        ArgumentCaptor<SimpleMailMessage> emailMessage = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailMessage.capture());
        Matcher codeMatcher = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)")
                .matcher(emailMessage.getValue().getText());
        assertTrue(codeMatcher.find());
        String invalidCode = "000000".equals(codeMatcher.group()) ? "000001" : "000000";
        mockMvc.perform(post("/api/v1/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "challengeId", challengeId, "otp", invalidCode))))
                .andExpect(status().isBadRequest());
        assertTrue(userRepository.findByEmail(email).isEmpty());
    }

    @Test
    void testRegistrationWithExistingEmailDoesNotPretendOtpWasSent() throws Exception {
        String email = "existing" + System.currentTimeMillis() + "@example.com";
        User existing = new User(
                email, passwordEncoder.encode("Password@123"), "Existing Customer", null, CustomerType.RETAIL);
        existing.setRoles(Set.of(Role.CUSTOMER));
        existing.setEmailVerified(true);
        userRepository.save(existing);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(
                        "An account with this email already exists. Please sign in."));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testRegistrationEmailDeliveryFailureIsReportedByStatusEndpoint() throws Exception {
        String email = "mailfailure" + System.currentTimeMillis() + "@example.com";
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest(email))))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String challengeId = objectMapper.readTree(response).path("data").path("challengeId").asText();

        awaitRegistrationEmailStatus(challengeId, "FAILED");
        assertTrue(userRepository.findByEmail(email).isEmpty(),
                "A failed OTP email must not create a customer account");
    }

    @Test
    void testUnverifiedCustomerCannotLogin() throws Exception {
        String email = "unverified" + System.currentTimeMillis() + "@example.com";
        User customer = new User(email, passwordEncoder.encode("Password@123"), "Unverified Customer", null, CustomerType.RETAIL);
        customer.setRoles(Set.of(Role.CUSTOMER));
        customer.setEmailVerified(false);
        userRepository.save(customer);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("not verified")));
    }

    private RegisterRequest registrationRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Password@123");
        request.setFullName("Test Registration");
        return request;
    }

    private void awaitRegistrationEmailStatus(String challengeId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            String response = mockMvc.perform(get("/api/v1/auth/register/status")
                            .param("challengeId", challengeId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String actualStatus = objectMapper.readTree(response).path("data").path("status").asText();
            if (expectedStatus.equals(actualStatus)) {
                return;
            }
            if ("FAILED".equals(actualStatus)) {
                throw new AssertionError("Registration email delivery failed unexpectedly");
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for registration email status " + expectedStatus);
    }

    private String authenticate(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
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
