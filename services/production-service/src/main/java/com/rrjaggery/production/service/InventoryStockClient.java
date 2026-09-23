package com.rrjaggery.production.service;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryStockClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockClient.class);

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.inventory.base-url:http://localhost:8084}")
    private String inventoryBaseUrl;

    public InventoryStockClient(RestTemplate restTemplate, JwtTokenProvider jwtTokenProvider) {
        this.restTemplate = restTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenProvider.generateToken(
                "production-service", "production-service@internal", List.of("PRODUCTION_MANAGER", "EMPLOYEE"), "INTERNAL"));
        return headers;
    }

    public Optional<InventoryItemSummary> getItemBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<ApiResponse<InventoryItemSummary>> response = restTemplate.exchange(
                    inventoryBaseUrl + "/api/v1/inventory/items/sku/" + sku.trim().toUpperCase(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<ApiResponse<InventoryItemSummary>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().isSuccess()) {
                return Optional.ofNullable(response.getBody().getData());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Inventory item with SKU {} not found in inventory service", sku);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to query inventory item for SKU {}: {}", sku, e.getMessage());
            throw new IllegalStateException("Failed to query inventory service for SKU " + sku + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void consumeRawMaterial(String sku, BigDecimal quantity, String batchNumber, String actor, String notes, String idempotencyKey) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required for material consumption.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Consumed quantity must be greater than 0.");
        }

        InventoryItemSummary item = getItemBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Raw material with SKU '" + sku + "' not found in inventory."));

        if (item.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient inventory for SKU '" + sku + "'. Available: " + item.getCurrentQuantity() + ", Required: " + quantity);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", item.getId());
        payload.put("quantity", quantity.negate());
        payload.put("reason", "PRODUCTION_CONSUMPTION");
        payload.put("referenceType", "PRODUCTION_BATCH");
        payload.put("referenceId", idempotencyKey != null ? idempotencyKey : batchNumber + "::CONSUMPTION::" + sku);
        payload.put("actor", actor == null || actor.isBlank() ? "PRODUCTION_MANAGER" : actor);
        payload.put("notes", notes == null ? "Raw material consumed for batch " + batchNumber : notes);

        executeStockAdjustment(payload, "consumption for SKU " + sku);
    }

    public void recordOutput(String sku, String productName, BigDecimal quantity, String batchNumber, String batchLot, String actor, String notes, String idempotencyKey) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required for production output.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Production output quantity must be greater than 0.");
        }

        Optional<InventoryItemSummary> existing = getItemBySku(sku);
        UUID itemId;
        if (existing.isEmpty()) {
            // Create item first if not present
            itemId = createInventoryItem(sku, productName != null ? productName : sku, "FINISHED_GOOD", "KG", batchLot);
        } else {
            itemId = existing.get().getId();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);
        payload.put("quantity", quantity);
        payload.put("reason", "PRODUCTION_OUTPUT");
        payload.put("referenceType", "PRODUCTION_BATCH");
        payload.put("referenceId", idempotencyKey != null ? idempotencyKey : batchNumber + "::OUTPUT::" + sku);
        payload.put("actor", actor == null || actor.isBlank() ? "PRODUCTION_MANAGER" : actor);
        payload.put("notes", notes == null ? "Finished goods produced from batch " + batchNumber + " (Lot: " + batchLot + ")" : notes);

        executeStockAdjustment(payload, "output for SKU " + sku);
    }

    public void recordWastage(String sku, BigDecimal quantity, String batchNumber, String wastageType, String actor, String reason, String idempotencyKey) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required for wastage recording.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Wastage quantity must be greater than 0.");
        }

        Optional<InventoryItemSummary> itemOpt = getItemBySku(sku);
        if (itemOpt.isEmpty()) {
            log.warn("Item with SKU {} not found in inventory during wastage adjustment, skipping remote inventory reduction.", sku);
            return;
        }

        InventoryItemSummary item = itemOpt.get();
        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", item.getId());
        payload.put("quantity", quantity.negate());
        payload.put("reason", "LOSS");
        payload.put("referenceType", "PRODUCTION_BATCH");
        payload.put("referenceId", idempotencyKey != null ? idempotencyKey : batchNumber + "::WASTAGE::" + sku);
        payload.put("actor", actor == null || actor.isBlank() ? "PRODUCTION_MANAGER" : actor);
        payload.put("notes", (reason != null ? reason : "Production wastage/loss: " + wastageType) + " for batch " + batchNumber);

        executeStockAdjustment(payload, "wastage for SKU " + sku);
    }

    private UUID createInventoryItem(String sku, String itemName, String itemType, String unitOfMeasure, String batchLot) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", sku.trim().toUpperCase());
        payload.put("itemName", itemName);
        payload.put("itemType", itemType);
        payload.put("unitOfMeasure", unitOfMeasure != null ? unitOfMeasure : "KG");
        payload.put("initialQuantity", BigDecimal.ZERO);
        payload.put("minimumStockLevel", BigDecimal.ZERO);
        payload.put("batchLot", batchLot);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createAuthHeaders());
        ResponseEntity<ApiResponse<InventoryItemSummary>> response = restTemplate.exchange(
                inventoryBaseUrl + "/api/v1/inventory/items",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<ApiResponse<InventoryItemSummary>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Failed to create inventory item for output SKU " + sku);
        }
        return response.getBody().getData().getId();
    }

    private void executeStockAdjustment(Map<String, Object> payload, String operationDescription) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createAuthHeaders());
        ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                inventoryBaseUrl + "/api/v1/inventory/stock/adjust",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Inventory service rejected stock adjustment for " + operationDescription);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InventoryItemSummary {
        private UUID id;
        private String sku;
        private String itemName;
        private String itemType;
        private BigDecimal currentQuantity = BigDecimal.ZERO;
        private BigDecimal minimumStockLevel = BigDecimal.ZERO;
        private String unitOfMeasure;
        private String batchLot;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public BigDecimal getCurrentQuantity() {
            return currentQuantity;
        }

        public void setCurrentQuantity(BigDecimal currentQuantity) {
            this.currentQuantity = currentQuantity;
        }

        public BigDecimal getMinimumStockLevel() {
            return minimumStockLevel;
        }

        public void setMinimumStockLevel(BigDecimal minimumStockLevel) {
            this.minimumStockLevel = minimumStockLevel;
        }

        public String getUnitOfMeasure() {
            return unitOfMeasure;
        }

        public void setUnitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
        }

        public String getBatchLot() {
            return batchLot;
        }

        public void setBatchLot(String batchLot) {
            this.batchLot = batchLot;
        }
    }
}
