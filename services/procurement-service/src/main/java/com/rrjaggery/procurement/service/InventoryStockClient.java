package com.rrjaggery.procurement.service;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryStockClient {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.inventory.base-url:http://localhost:8084}")
    private String inventoryBaseUrl;

    public InventoryStockClient(RestTemplate restTemplate, JwtTokenProvider jwtTokenProvider) {
        this.restTemplate = restTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void receiveStock(String sku, BigDecimal quantity, String referenceType, String referenceId,
                            String actor, String notes) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required for inventory receipt.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Inventory receipt quantity must be greater than 0.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", sku.trim().toUpperCase());
        payload.put("quantity", quantity);
        payload.put("referenceType", referenceType == null ? "PURCHASE_ORDER" : referenceType);
        payload.put("referenceId", referenceId);
        payload.put("actor", actor == null || actor.isBlank() ? "PROCUREMENT" : actor);
        payload.put("notes", notes == null ? "Goods receipt recorded by procurement service." : notes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenProvider.generateToken(
                "procurement-service", "procurement-service@internal", java.util.List.of("EMPLOYEE"), "INTERNAL"));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                inventoryBaseUrl + "/api/v1/inventory/stock/receive",
                request,
                ApiResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Inventory service rejected the stock receipt for SKU " + sku);
        }
    }
}
