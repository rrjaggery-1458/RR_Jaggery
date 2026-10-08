package com.rrjaggery.inventoryprocurement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InventoryProcurementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void inventoryHealthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("inventory_schema"));
    }

    @Test
    void procurementHealthCheckReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/procurement/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("procurement_schema"));
    }

    @Test
    void unauthenticatedInventoryEndpointsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/items"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedProcurementEndpointsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/procurement/orders"))
                .andExpect(status().isForbidden());
    }
}
