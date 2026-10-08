package com.rrjaggery.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.notification.dto.CreateNotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void customerRoleAccessIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@rrjaggery.com", roles = {"ADMIN"})
    void createAndRetrieveNotificationWorkflow() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setType("LOW_STOCK");
        request.setSeverity("WARNING");
        request.setTitle("Low Stock Alert");
        request.setMessage("Organic Jaggery 1kg is below minimum threshold");
        request.setReferenceType("INVENTORY");
        request.setReferenceId("SKU-JAG-001");
        request.setRole("ADMIN");

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Low Stock Alert"))
                .andExpect(jsonPath("$.data.read").value(false));

        // Verify unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").isNumber());

        // Verify mark-all-as-read
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "manager@rrjaggery.com", roles = {"MANAGER"})
    void syncOperationalAlertsWorkflow() throws Exception {
        CreateNotificationRequest alert = new CreateNotificationRequest();
        alert.setType("PENDING_ORDER");
        alert.setSeverity("INFO");
        alert.setTitle("Operational Alert Test");
        alert.setMessage("Order created through consolidation workflow");
        alert.setReferenceType("ORDER");
        alert.setReferenceId("ORD-CONSOLIDATION-01");
        alert.setRole("MANAGER");

        mockMvc.perform(post("/api/v1/notifications/sync-operational-alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(alert))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
