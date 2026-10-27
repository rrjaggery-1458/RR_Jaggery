package com.rrjaggery.commerce.service;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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

    public void deductSaleStock(String sku, BigDecimal quantity, String orderReference, String actor) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", sku);
        payload.put("quantity", quantity);
        payload.put("referenceType", "ORDER");
        payload.put("referenceId", orderReference);
        payload.put("actor", actor == null || actor.isBlank() ? "COMMERCE" : actor);
        payload.put("notes", "Finished-goods deduction for confirmed commerce order.");

        HttpHeaders headers = buildAuthHeaders();
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                inventoryBaseUrl + "/api/v1/inventory/stock/sale",
                new HttpEntity<>(payload, headers),
                ApiResponse.class
        );
        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Inventory service rejected sale stock for SKU " + sku);
        }
    }

    /**
     * Returns the current available quantity for the given SKU from the inventory service.
     * Returns BigDecimal.ZERO if the item is not found.
     */
    public BigDecimal checkAvailableStock(String sku) {
        HttpHeaders headers = buildAuthHeaders();
        try {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    inventoryBaseUrl + "/api/v1/inventory/items/sku/" + sku,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                Object qty = response.getBody().getData().get("currentQuantity");
                if (qty != null) {
                    return new BigDecimal(qty.toString());
                }
            }
            return BigDecimal.ZERO;
        } catch (HttpClientErrorException.NotFound e) {
            return BigDecimal.ZERO;
        }
    }

    public void restoreSaleStock(String sku, BigDecimal quantity, String orderReference, String actor) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", sku);
        payload.put("quantity", quantity);
        payload.put("referenceType", "ORDER");
        payload.put("referenceId", orderReference);
        payload.put("actor", actor == null || actor.isBlank() ? "COMMERCE" : actor);
        payload.put("notes", "Idempotent finished-goods restoration for cancelled order.");

        HttpHeaders headers = buildAuthHeaders();
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                inventoryBaseUrl + "/api/v1/inventory/stock/restore",
                new HttpEntity<>(payload, headers),
                ApiResponse.class
        );
        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Inventory service rejected stock restoration for SKU " + sku);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenProvider.generateToken(
                "commerce-service", "commerce-service@internal",
                java.util.List.of(
                        com.rrjaggery.common.security.SecurityConstants.ROLE_MANAGER,
                        com.rrjaggery.common.security.SecurityConstants.ROLE_ADMIN),
                "INTERNAL"));
        return headers;
    }
}
