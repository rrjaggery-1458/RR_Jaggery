package com.rrjaggery.commerce.controller;

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
public class PublicCatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetCategoriesPublic() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetProductsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testUnauthorizedAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/admin/products"))
                .andExpect(status().isForbidden());
    }
}
