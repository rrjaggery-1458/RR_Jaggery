package com.rrjaggery.customerfinance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerFinanceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void customerHealthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/customers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("customer_schema"));
    }

    @Test
    void financeHealthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/finance/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("finance_schema"));
    }

    @Test
    void notificationHealthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("notification_schema"));
    }

    @Test
    void unauthenticatedCustomerEndpointsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedFinanceEndpointsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/finance/expenses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedNotificationEndpointsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@rrjaggery.com", roles = {"ADMIN"})
    void adminCanAccessNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin@rrjaggery.com", roles = {"ADMIN"})
    void adminCanAccessNotificationUnreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").isNumber());
    }
}
