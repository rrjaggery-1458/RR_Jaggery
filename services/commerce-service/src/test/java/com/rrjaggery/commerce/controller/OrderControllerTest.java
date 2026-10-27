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
import com.rrjaggery.commerce.repository.StockOutboxRepository;
import com.rrjaggery.commerce.service.InventoryStockClient;
import com.rrjaggery.commerce.service.RazorpayPaymentService;
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
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private StockOutboxRepository stockOutboxRepository;

    @MockBean
    private InventoryStockClient inventoryStockClient;

    @MockBean
    private RazorpayPaymentService razorpayService;

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
        org.mockito.Mockito.when(inventoryStockClient.checkAvailableStock(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new BigDecimal("100.000"));
        org.mockito.Mockito.when(razorpayService.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(razorpayService.verifyPaymentSignature(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    void testCheckoutWithEmptyCartFails() throws Exception {
        AddressDto address = new AddressDto("Test Buyer", "+91 9999988888", "Blue House", "123 Main St", "Mandya", "Karnataka", "571401", "Near market");
        CheckoutRequest req = new CheckoutRequest(address, PaymentMethod.CASH_ON_DELIVERY, "Quick delivery");

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

        // 2. Checkout via verified Razorpay
        AddressDto address = new AddressDto("Buyer Ramesh", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Fragile jaggery blocks",
                "order_rzp_123", "pay_rzp_456", "sig_rzp_789");

        MvcResult result = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.shippingAddress.apartmentName").value("Farm House"))
                .andExpect(jsonPath("$.data.shippingAddress.nearestLandmark").value("Near temple"))
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
                .andExpect(jsonPath("$.data.sellerName").value("RR Jaggery"))
                .andExpect(jsonPath("$.data.totalTax").value(0))
                .andExpect(jsonPath("$.data.shippingAddress.apartmentName").value("Farm House"))
                .andExpect(jsonPath("$.data.shippingAddress.nearestLandmark").value("Near temple"))
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

    @Test
    void pendingOrderDeliveredLater_deductsStockUsingKilogramWeight() throws Exception {
        testProduct.setRetailPrice(new BigDecimal("160.00"));
        testProduct.setUnitWeightKg(BigDecimal.ONE);
        productRepository.save(testProduct);

        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 2);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.CASH_ON_DELIVERY, "Test order");
        MvcResult checkout = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.subtotalAmount").value(320.0))
                .andExpect(jsonPath("$.data.taxAmount").value(0.0))
                .andExpect(jsonPath("$.data.shippingAmount").value(50.0))
                .andExpect(jsonPath("$.data.packagingFee").value(10.0))
                .andExpect(jsonPath("$.data.totalAmount").value(380.0))
                .andReturn();
        String orderId = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .path("data").path("id").asText();
        String orderNumber = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .path("data").path("orderNumber").asText();

        assertTrue(stockOutboxRepository.findAll().stream()
                .noneMatch(command -> command.getReferenceId().startsWith(orderNumber + ":")));

        UpdateOrderStatusRequest delivered = new UpdateOrderStatusRequest(OrderStatus.DELIVERED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delivered)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        var stockCommand = stockOutboxRepository.findAll().stream()
                .filter(command -> command.getReferenceId().startsWith(orderNumber + ":"))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("2.000"), stockCommand.getQuantity());
        org.mockito.Mockito.verify(inventoryStockClient).deductSaleStock(
                org.mockito.ArgumentMatchers.eq(testProduct.getSku()),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("2.000")),
                org.mockito.ArgumentMatchers.startsWith(orderNumber + ":"),
                org.mockito.ArgumentMatchers.eq("COMMERCE"));
    }

    @Test
    void cannotMarkOrderDeliveredWhenStockInsufficient() throws Exception {
        testProduct.setRetailPrice(new BigDecimal("160.00"));
        testProduct.setUnitWeightKg(BigDecimal.ONE);
        productRepository.save(testProduct);

        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 5);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.CASH_ON_DELIVERY, "Test order");
        MvcResult checkout = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String orderId = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Simulate 0 available stock in inventory
        org.mockito.Mockito.when(inventoryStockClient.checkAvailableStock(testProduct.getSku()))
                .thenReturn(BigDecimal.ZERO);

        UpdateOrderStatusRequest delivered = new UpdateOrderStatusRequest(OrderStatus.DELIVERED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delivered)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString(
                        "The quantity of this product in the order is more than what we have in our stock kindly add production then try again to mark it delivered")));
    }

    @Test
    void cannotMarkOrderDispatchedWhenStockInsufficient() throws Exception {
        testProduct.setRetailPrice(new BigDecimal("160.00"));
        testProduct.setUnitWeightKg(BigDecimal.ONE);
        productRepository.save(testProduct);

        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 5);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Test order",
                "order_rzp_123", "pay_rzp_456", "sig_rzp_789");
        MvcResult checkout = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn();

        String orderId = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Simulate 2 KG available stock, but order needs 5 KG
        org.mockito.Mockito.when(inventoryStockClient.checkAvailableStock(testProduct.getSku()))
                .thenReturn(new BigDecimal("2.000"));

        UpdateOrderStatusRequest dispatched = new UpdateOrderStatusRequest(OrderStatus.DISPATCHED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatched)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString(
                        "The quantity of this product in the order is more than what we have in our stock kindly add production then try again to mark it dispatched")));
    }

    @Test
    void dispatchedOrderRollback_restoresStock() throws Exception {
        testProduct.setRetailPrice(new BigDecimal("160.00"));
        testProduct.setUnitWeightKg(BigDecimal.ONE);
        productRepository.save(testProduct);

        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest checkoutReq = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Test rollback",
                "order_rzp_123", "pay_rzp_456", "sig_rzp_789");
        MvcResult checkout = mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andReturn();
        var orderData = objectMapper.readTree(checkout.getResponse().getContentAsString()).path("data");
        String orderId = orderData.path("id").asText();
        String orderNumber = orderData.path("orderNumber").asText();

        // Mark DISPATCHED (deducts stock)
        UpdateOrderStatusRequest dispatch = new UpdateOrderStatusRequest(OrderStatus.DISPATCHED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));

        // Roll back to CONFIRMED (restores stock)
        UpdateOrderStatusRequest rollback = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        mockMvc.perform(put("/api/v1/commerce/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        var commands = stockOutboxRepository.findAll().stream()
                .filter(command -> command.getReferenceId().startsWith(orderNumber + ":"))
                .toList();
        assertEquals(2, commands.size());
        var sale = commands.stream().filter(command -> command.getOperation().name().equals("DEDUCT"))
                .findFirst().orElseThrow();
        var restoration = commands.stream().filter(command -> command.getOperation().name().equals("RESTORE"))
                .findFirst().orElseThrow();
        assertEquals(sale.getCommandKey(), restoration.getDependsOnCommandKey());
        org.mockito.Mockito.verify(inventoryStockClient).restoreSaleStock(
                org.mockito.ArgumentMatchers.eq(testProduct.getSku()),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("1.000")),
                org.mockito.ArgumentMatchers.startsWith(orderNumber + ":"),
                org.mockito.ArgumentMatchers.eq("COMMERCE"));
    }

    @Test
    void testCheckoutWithRemovedTestPaymentFails() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        String rawJson = """
            {
                "shippingAddress": {
                    "recipientName": "Buyer",
                    "phone": "+91 9888877777",
                    "apartmentName": "Farm House",
                    "streetAddress": "Farm House 4",
                    "city": "Mandya",
                    "state": "Karnataka",
                    "postalCode": "571401"
                },
                "paymentMethod": "TEST_PAYMENT"
            }
            """;

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckoutWithMissingPaymentMethodFails() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        String rawJson = """
            {
                "shippingAddress": {
                    "recipientName": "Buyer",
                    "phone": "+91 9888877777",
                    "apartmentName": "Farm House",
                    "streetAddress": "Farm House 4",
                    "city": "Mandya",
                    "state": "Karnataka",
                    "postalCode": "571401"
                }
            }
            """;

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckoutRazorpayMissingSignatureFails() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest req = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Notes");

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void testCheckoutRazorpayInvalidSignatureFails() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        org.mockito.Mockito.when(razorpayService.verifyPaymentSignature(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest req = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Notes", "order_1", "pay_1", "invalid_sig");

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SIGNATURE_MISMATCH"));
    }

    @Test
    void testCheckoutRazorpayDisabledFails() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testProduct.getId(), 1);
        mockMvc.perform(post("/api/v1/commerce/cart/items")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        org.mockito.Mockito.when(razorpayService.isEnabled()).thenReturn(false);

        AddressDto address = new AddressDto("Buyer", "+91 9888877777", "Farm House", "Farm House 4", "Mandya", "Karnataka", "571401", "Near temple");
        CheckoutRequest req = new CheckoutRequest(address, PaymentMethod.RAZORPAY, "Notes", "order_1", "pay_1", "sig_1");

        mockMvc.perform(post("/api/v1/commerce/orders/checkout")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RAZORPAY_DISABLED"));
    }

    // ── CORS Preflight Tests (Phase 2.2A) ────────────────────────────────────

    @Test
    void testCorsProductionOriginsReceiveHeaders() throws Exception {
        // Preflight request from production root domain must receive explicit Allow-Origin
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "https://rrdentalneeds.online")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertEquals("https://rrdentalneeds.online", allowOrigin,
                            "Production root domain should receive explicit Access-Control-Allow-Origin header");
                });

        // Preflight request from www subdomain must also receive explicit Allow-Origin
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "https://www.rrdentalneeds.online")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertEquals("https://www.rrdentalneeds.online", allowOrigin,
                            "Production www subdomain should receive explicit Access-Control-Allow-Origin header");
                });
    }

    @Test
    void testCorsLocalDevOriginReceivesHeader() throws Exception {
        // Configured local development origin (http://localhost:5173)
        // should receive Access-Control-Allow-Origin in preflight response.
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertEquals("http://localhost:5173", allowOrigin,
                            "Allowed dev origin should receive explicit Access-Control-Allow-Origin header");
                });
    }

    @Test
    void testCorsDisallowedOriginReceivesNoHeader() throws Exception {
        // An untrusted origin must NOT receive Access-Control-Allow-Origin in preflight response.
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertNull(allowOrigin,
                            "Untrusted origin must not receive Access-Control-Allow-Origin");
                });
    }

    @Test
    void testCorsAllowedMethodsAndHeaders() throws Exception {
        // Preflight request must confirm allowed methods and required headers
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "https://rrdentalneeds.online")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String allowMethods = result.getResponse().getHeader("Access-Control-Allow-Methods");
                    org.junit.jupiter.api.Assertions.assertNotNull(allowMethods, "Allowed methods must be present");
                    org.junit.jupiter.api.Assertions.assertTrue(allowMethods.contains("GET"), "Methods must include GET");
                    org.junit.jupiter.api.Assertions.assertTrue(allowMethods.contains("POST"), "Methods must include POST");
                    String allowHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
                    org.junit.jupiter.api.Assertions.assertNotNull(allowHeaders, "Allowed headers must be present");
                    org.junit.jupiter.api.Assertions.assertTrue(allowHeaders.contains("Authorization"), "Headers must include Authorization");
                    org.junit.jupiter.api.Assertions.assertTrue(allowHeaders.contains("Content-Type"), "Headers must include Content-Type");
                });
    }

    @Test
    void testCorsNoWildcardInAllowedOrigins() throws Exception {
        // The wildcard '*' must not appear in the Access-Control-Allow-Origin header.
        // Perform a preflight from the allowed origin and assert the response is an explicit value.
        mockMvc.perform(options("/api/v1/commerce/products")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(result -> {
                    String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    org.junit.jupiter.api.Assertions.assertNotEquals("*", allowOrigin,
                            "Access-Control-Allow-Origin must not be a wildcard '*'");
                });
    }
}
