package com.rrjaggery.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.commerce.dto.AddToCartRequest;
import com.rrjaggery.commerce.dto.AddressDto;
import com.rrjaggery.commerce.dto.CheckoutRequest;
import com.rrjaggery.commerce.dto.UpdateOrderStatusRequest;
import com.rrjaggery.commerce.entity.OrderStatus;
import com.rrjaggery.commerce.entity.PaymentMethod;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.repository.ProductRepository;
import com.rrjaggery.commerce.service.InventoryStockClient;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private InventoryStockClient inventoryStockClient;

    private String customerToken;
    private String adminToken;
    private UUID customerId;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerToken = "Bearer " + jwtTokenProvider.generateToken(
                customerId.toString(),
                "ordercustomer@test.com",
                List.of("CUSTOMER"),
                "RETAIL"
        );

        adminToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(),
                "admin@rrjaggery.com",
                List.of("ADMIN", "CUSTOMER"),
                "INTERNAL"
        );

        testProduct = productRepository.findAll().stream().findFirst().orElseThrow();
    }

    @Test
    void testCheckoutWithEmptyCartFails() throws Exception {
        AddressDto address = new AddressDto("Test Buyer", "+91 9999988888", "123 Main St", "Mandya", "Karnataka", "571401");
        CheckoutRequest req = new CheckoutRequest(address, PaymentMethod.TEST_PAYMENT, "Quick delivery");

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFullOrderLifecycleAndInvoice() throws Exception {
        // 1. Add item to cart
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 3);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        // 2. Checkout
        AddressDto address = new AddressDto("Buyer Ramesh", "+91 9888877777", "Farm House 4", "Mandya", "Karnataka", "571401");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.TEST_PAYMENT, "Fragile jaggery blocks");

        MvcResult result = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String orderId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        // 3. Customer gets their orders
        mockMvc.perform(get("/api/v1/commerce/orders")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(orderId));

        // 4. Customer views invoice
        mockMvc.perform(get("/api/v1/commerce/orders/" + orderId + "/invoice")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sellerGstin").value("29AABCR1234F1Z5"))
                .andExpect(jsonPath("$.data.orderId").value(orderId));

        // 5. Admin updates status to PROCESSING -> DISPATCHED
        UpdateOrderStatusRequest updateReq = new UpdateOrderStatusRequest(OrderStatus.DISPATCHED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));
    }
}
