package com.rrjaggery.finance.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Component
public class FinanceOrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FinanceOrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.rrjaggery.customerledger.service.OfflineOrderService offlineOrderService;

    @Value("${app.services.customer-ledger-base-url:http://customer-finance-service:8083}")
    private String customerLedgerBaseUrl;

    @Value("${app.services.commerce-base-url:http://commerce-service:8082}")
    private String commerceBaseUrl;

    @Value("${app.services.inventory-base-url:http://inventory-procurement-service:8084}")
    private String inventoryBaseUrl;

    public FinanceOrderServiceClient(RestTemplate restTemplate, JwtTokenProvider jwtTokenProvider) {
        this.restTemplate = restTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenProvider.generateToken(
                "finance-service", "finance-service@internal", List.of("ROLE_MANAGER", "ROLE_ADMIN"), "INTERNAL"));
        return headers;
    }

    public List<RemoteOfflineOrderDto> getOfflineOrders() {
        if (offlineOrderService != null) {
            try {
                List<com.rrjaggery.customerledger.dto.OfflineOrderDto> dtoList = offlineOrderService.getAllOfflineOrders();
                List<RemoteOfflineOrderDto> result = new ArrayList<>();
                for (com.rrjaggery.customerledger.dto.OfflineOrderDto dto : dtoList) {
                    RemoteOfflineOrderDto r = new RemoteOfflineOrderDto();
                    r.id = dto.getId();
                    r.orderNumber = dto.getOrderNumber();
                    r.customerName = dto.getCustomerName();
                    r.businessName = dto.getBusinessName();
                    r.invoiceNumber = dto.getInvoiceNumber();
                    r.subtotalAmount = dto.getSubtotalAmount() != null ? dto.getSubtotalAmount() : BigDecimal.ZERO;
                    r.taxAmount = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;
                    r.deliveryCharges = dto.getDeliveryCharges() != null ? dto.getDeliveryCharges() : BigDecimal.ZERO;
                    r.totalAmount = dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO;
                    r.paidAmount = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
                    r.outstandingAmount = dto.getOutstandingAmount() != null ? dto.getOutstandingAmount() : BigDecimal.ZERO;
                    r.orderDate = dto.getOrderDate();
                    r.status = dto.getStatus();
                    r.paymentStatus = dto.getPaymentStatus();
                    r.paymentMethod = dto.getPaymentMethod();
                    r.orderType = dto.getOrderType();
                    if (dto.getItems() != null) {
                        for (com.rrjaggery.customerledger.dto.OfflineOrderItemDto item : dto.getItems()) {
                            RemoteOfflineOrderItemDto ri = new RemoteOfflineOrderItemDto();
                            ri.productId = item.getProductId();
                            ri.productName = item.getProductName();
                            ri.sku = item.getSku();
                            ri.unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                            ri.quantity = item.getQuantity();
                            ri.unitWeightKg = item.getUnitWeightKg() != null ? item.getUnitWeightKg() : BigDecimal.ZERO;
                            ri.lineTotal = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                            r.items.add(ri);
                        }
                    }
                    result.add(r);
                }
                return result;
            } catch (Exception e) {
                log.warn("Direct offline orders fetch failed, falling back to HTTP: {}", e.getMessage());
            }
        }
        String url = customerLedgerBaseUrl + "/api/v1/customers/offline-orders";
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List<RemoteOfflineOrderDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<RemoteOfflineOrderDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch offline orders from {}: {}", url, e.getMessage());
            // Fallback: try localhost if running locally outside docker
            if (!customerLedgerBaseUrl.contains("localhost")) {
                try {
                    String fallbackUrl = "http://localhost:8083/api/v1/customers/offline-orders";
                    HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
                    ResponseEntity<List<RemoteOfflineOrderDto>> response = restTemplate.exchange(
                            fallbackUrl,
                            HttpMethod.GET,
                            request,
                            new ParameterizedTypeReference<List<RemoteOfflineOrderDto>>() {}
                    );
                    return response.getBody() != null ? response.getBody() : Collections.emptyList();
                } catch (Exception ex) {
                    log.error("Fallback offline orders fetch failed: {}", ex.getMessage());
                }
            }
            return Collections.emptyList();
        }
    }

    public List<RemoteCommerceOrderDto> getCommerceOrders() {
        String url = commerceBaseUrl + "/api/v1/commerce/admin/orders";
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<ApiResponse<List<RemoteCommerceOrderDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<ApiResponse<List<RemoteCommerceOrderDto>>>() {}
            );
            return (response.getBody() != null && response.getBody().getData() != null)
                    ? response.getBody().getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch commerce orders from {}: {}", url, e.getMessage());
            if (!commerceBaseUrl.contains("localhost")) {
                try {
                    String fallbackUrl = "http://localhost:8082/api/v1/commerce/admin/orders";
                    HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
                    ResponseEntity<ApiResponse<List<RemoteCommerceOrderDto>>> response = restTemplate.exchange(
                            fallbackUrl,
                            HttpMethod.GET,
                            request,
                            new ParameterizedTypeReference<ApiResponse<List<RemoteCommerceOrderDto>>>() {}
                    );
                    return (response.getBody() != null && response.getBody().getData() != null)
                            ? response.getBody().getData() : Collections.emptyList();
                } catch (Exception ex) {
                    log.error("Fallback commerce orders fetch failed: {}", ex.getMessage());
                }
            }
            return Collections.emptyList();
        }
    }

    public List<RemoteInventoryItemDto> getInventoryItems() {
        String url = inventoryBaseUrl + "/api/v1/inventory/items";
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<ApiResponse<List<RemoteInventoryItemDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<ApiResponse<List<RemoteInventoryItemDto>>>() {}
            );
            return (response.getBody() != null && response.getBody().getData() != null)
                    ? response.getBody().getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch inventory items from {}: {}", url, e.getMessage());
            if (!inventoryBaseUrl.contains("localhost")) {
                try {
                    String fallbackUrl = "http://localhost:8084/api/v1/inventory/items";
                    HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
                    ResponseEntity<ApiResponse<List<RemoteInventoryItemDto>>> response = restTemplate.exchange(
                            fallbackUrl,
                            HttpMethod.GET,
                            request,
                            new ParameterizedTypeReference<ApiResponse<List<RemoteInventoryItemDto>>>() {}
                    );
                    return (response.getBody() != null && response.getBody().getData() != null)
                            ? response.getBody().getData() : Collections.emptyList();
                } catch (Exception ex) {
                    log.error("Fallback inventory items fetch failed: {}", ex.getMessage());
                }
            }
            return Collections.emptyList();
        }
    }

    public List<RemoteProductDto> getCommerceProducts() {
        String url = commerceBaseUrl + "/api/v1/commerce/products";
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<ApiResponse<List<RemoteProductDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<ApiResponse<List<RemoteProductDto>>>() {}
            );
            return (response.getBody() != null && response.getBody().getData() != null)
                    ? response.getBody().getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch commerce products from {}: {}", url, e.getMessage());
            if (!commerceBaseUrl.contains("localhost")) {
                try {
                    String fallbackUrl = "http://localhost:8082/api/v1/commerce/products";
                    HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
                    ResponseEntity<ApiResponse<List<RemoteProductDto>>> response = restTemplate.exchange(
                            fallbackUrl,
                            HttpMethod.GET,
                            request,
                            new ParameterizedTypeReference<ApiResponse<List<RemoteProductDto>>>() {}
                    );
                    return (response.getBody() != null && response.getBody().getData() != null)
                            ? response.getBody().getData() : Collections.emptyList();
                } catch (Exception ex) {
                    log.error("Fallback commerce products fetch failed: {}", ex.getMessage());
                }
            }
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteOfflineOrderDto {
        public UUID id;
        public String orderNumber;
        public String customerName;
        public String businessName;
        public String invoiceNumber;
        public BigDecimal subtotalAmount = BigDecimal.ZERO;
        public BigDecimal taxAmount = BigDecimal.ZERO;
        public BigDecimal deliveryCharges = BigDecimal.ZERO;
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public BigDecimal paidAmount = BigDecimal.ZERO;
        public BigDecimal outstandingAmount = BigDecimal.ZERO;
        public Instant orderDate;
        public String status;
        public String paymentStatus;
        public String paymentMethod;
        public String orderType; // "WHOLESALE" or "MANUAL_RETAIL"
        public List<RemoteOfflineOrderItemDto> items = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteOfflineOrderItemDto {
        public UUID productId;
        public String productName;
        public String sku;
        public BigDecimal unitPrice = BigDecimal.ZERO;
        public int quantity;
        public BigDecimal unitWeightKg = BigDecimal.ZERO;
        public BigDecimal lineTotal = BigDecimal.ZERO;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteCommerceOrderDto {
        public UUID id;
        public String orderNumber;
        public String customerName;
        public String customerEmail;
        public String customerType;
        public BigDecimal subtotalAmount = BigDecimal.ZERO;
        public BigDecimal taxAmount = BigDecimal.ZERO;
        public BigDecimal shippingAmount = BigDecimal.ZERO;
        public BigDecimal discountAmount = BigDecimal.ZERO;
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
        public String paymentStatus; // PENDING, PAID, FAILED, REFUNDED
        public Instant createdAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteInventoryItemDto {
        public UUID id;
        public String sku;
        public String itemName;
        public String itemType; // "FINISHED_GOOD", "RAW_MATERIAL"
        public BigDecimal currentQuantity = BigDecimal.ZERO;
        public String unitOfMeasure;
        public String status;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteProductDto {
        public UUID id;
        public String name;
        public String sku;
        public BigDecimal retailPrice = BigDecimal.ZERO;
        public BigDecimal wholesalePrice = BigDecimal.ZERO;
        public boolean active;
    }
}
