package com.rrjaggery.customerledger.service;

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
import java.util.List;
import java.util.Map;

@Component
public class OfflineInventoryStockClient {
    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.services.inventory-base-url:http://inventory-procurement-service:8084}")
    private String inventoryBaseUrl;

    public OfflineInventoryStockClient(RestTemplate restTemplate, JwtTokenProvider jwtTokenProvider) {
        this.restTemplate = restTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void deductOfflineOrderStock(String sku, BigDecimal quantityKg, String referenceId) {
        postStockCommand("/api/v1/inventory/stock/sale", sku, quantityKg, referenceId,
                "Finished-goods deduction for confirmed offline order.");
    }

    public void restoreOfflineOrderStock(String sku, BigDecimal quantityKg, String referenceId) {
        postStockCommand("/api/v1/inventory/stock/restore", sku, quantityKg, referenceId,
                "Idempotent finished-goods restoration for cancelled offline order.");
    }

    /**
     * Returns the current available quantity for the given SKU from the inventory service.
     * Returns BigDecimal.ZERO if the item is not found (no inventory item registered yet).
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

    private void postStockCommand(String endpoint, String sku, BigDecimal quantityKg,
                                   String referenceId, String notes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", sku);
        payload.put("quantity", quantityKg);
        payload.put("referenceType", "ORDER");
        payload.put("referenceId", referenceId);
        payload.put("actor", "customer-finance-service");
        payload.put("notes", notes);

        HttpHeaders headers = buildAuthHeaders();
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                inventoryBaseUrl + endpoint,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});
        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Inventory service rejected stock command for SKU " + sku);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenProvider.generateToken(
                "customer-finance-service", "customer-finance-service@internal",
                List.of("ROLE_MANAGER", "ROLE_ADMIN"), "INTERNAL"));
        return headers;
    }
}
