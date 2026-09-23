package com.rrjaggery.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.commerce.dto.AddToCartRequest;
import com.rrjaggery.commerce.dto.UpdateCartItemRequest;
import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductGrade;
import com.rrjaggery.commerce.repository.CategoryRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String customerToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        customerToken = "Bearer " + jwtTokenProvider.generateToken(
                userId.toString(),
                "customer@test.com",
                List.of("CUSTOMER"),
                "RETAIL"
        );

        testProduct = productRepository.findAll().stream().findFirst().orElseThrow();
    }

    @Test
    void testGetEmptyCart() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/cart")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0.0));
    }

    @Test
    void testAddToCartAndGetCart() throws Exception {
        AddToCartRequest req = new AddToCartRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(2));

        mockMvc.perform(get("/api/v1/commerce/cart")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value(testProduct.getName()));
    }

    @Test
    void testUnauthorizedCartAccess() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/cart"))
                .andExpect(status().isForbidden());
    }
}
