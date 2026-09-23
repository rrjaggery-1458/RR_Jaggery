package com.rrjaggery.inventory.service;

import com.rrjaggery.inventory.dto.CreateInventoryItemRequest;
import com.rrjaggery.inventory.dto.ReconcileInventoryRequest;
import com.rrjaggery.inventory.dto.ReceiveStockRequest;
import com.rrjaggery.inventory.dto.SaleStockRequest;
import com.rrjaggery.inventory.dto.StockAdjustmentRequest;
import com.rrjaggery.inventory.entity.InventoryItem;
import com.rrjaggery.inventory.entity.ItemType;
import com.rrjaggery.inventory.entity.MovementType;
import com.rrjaggery.inventory.entity.StockMovement;
import com.rrjaggery.inventory.repository.InventoryItemRepository;
import com.rrjaggery.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            StockMovementRepository stockMovementRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getItems() {
        return inventoryItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public InventoryItem getItem(UUID id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + id));
    }

    @Transactional(readOnly = true)
    public InventoryItem getItemBySku(String sku) {
        String normalizedSku = normalizeSku(sku);
        return inventoryItemRepository.findBySku(normalizedSku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found for SKU: " + sku));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemRepository.findAll().stream()
                .filter(item -> item.getCurrentQuantity().compareTo(item.getMinimumStockLevel()) <= 0)
                .toList();
    }

    @Transactional
    public InventoryItem reconcileStock(ReconcileInventoryRequest request) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("A reconciliation reason is required.");
        }
        if (request.getCountedQuantity() == null || request.getCountedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted quantity cannot be negative.");
        }

        InventoryItem item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + request.getItemId()));

        BigDecimal delta = request.getCountedQuantity().subtract(item.getCurrentQuantity());
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return item;
        }

        StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
        adjustment.setItemId(item.getId());
        adjustment.setQuantity(delta);
        adjustment.setReason(request.getReason());
        adjustment.setReferenceType("RECONCILIATION");
        adjustment.setReferenceId("RECON-" + item.getId());
        adjustment.setActor(request.getActor() == null || request.getActor().isBlank() ? "ADMIN" : request.getActor());
        adjustment.setNotes(request.getNotes());

        return adjustStock(adjustment);
    }

    @Transactional
    public InventoryItem createItem(CreateInventoryItemRequest request) {
        String normalizedSku = normalizeSku(request.getSku());
        if (inventoryItemRepository.existsBySku(normalizedSku)) {
            throw new IllegalArgumentException("Inventory item already exists for SKU: " + normalizedSku);
        }

        BigDecimal initialQuantity = request.getInitialQuantity() == null ? BigDecimal.ZERO : request.getInitialQuantity();
        if (initialQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative.");
        }

        InventoryItem item = new InventoryItem();
        item.setSku(normalizedSku);
        item.setItemName(request.getItemName().trim());
        item.setItemType(parseItemType(request.getItemType()));
        item.setUnitOfMeasure(request.getUnitOfMeasure() == null || request.getUnitOfMeasure().isBlank() ? "KG" : request.getUnitOfMeasure().trim());
        item.setCurrentQuantity(initialQuantity);
        item.setMinimumStockLevel(request.getMinimumStockLevel() == null ? BigDecimal.ZERO : request.getMinimumStockLevel());
        item.setBatchLot(request.getBatchLot());
        item.setSourceReference(request.getSourceReference());
        item.setStatus("ACTIVE");

        InventoryItem saved = inventoryItemRepository.save(item);

        if (initialQuantity.compareTo(BigDecimal.ZERO) > 0) {
            recordMovement(saved, MovementType.OTHER, initialQuantity,
                    "INITIAL_STOCK", "INITIAL", "INITIAL", "SYSTEM", "Initial stock entry", "INITIAL_STOCK_" + saved.getId());
        }

        return saved;
    }

    @Transactional
    public InventoryItem adjustStock(StockAdjustmentRequest request) {
        if (request.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity delta is required.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Adjustment reason is required.");
        }

        InventoryItem item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + request.getItemId()));

        BigDecimal current = item.getCurrentQuantity();
        BigDecimal next = current.add(request.getQuantity());
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative stock is not allowed for item: " + item.getSku());
        }

        item.setCurrentQuantity(next);
        item.setUpdatedAt(java.time.Instant.now());
        InventoryItem saved = inventoryItemRepository.save(item);

        recordMovement(saved, resolveMovementType(request.getReason()), request.getQuantity(),
                request.getReason(), request.getReferenceType(), request.getReferenceId(),
                request.getActor() == null || request.getActor().isBlank() ? "SYSTEM" : request.getActor(),
                request.getNotes(), buildIdempotencyKey(request.getReferenceType(), request.getReferenceId(), request.getReason()));

        return saved;
    }

    @Transactional
    public InventoryItem receiveStock(ReceiveStockRequest request) {
        String sku = normalizeSku(request.getSku());
        InventoryItem item = inventoryItemRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found for SKU: " + sku));

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Receive quantity must be greater than 0.");
        }

        String idempotencyKey = buildIdempotencyKey(request.getReferenceType(), request.getReferenceId(), "GOODS_RECEIPT");
        if (stockMovementRepository.existsByIdempotencyKey(idempotencyKey)) {
            return item;
        }

        BigDecimal updated = item.getCurrentQuantity().add(request.getQuantity());
        item.setCurrentQuantity(updated);
        item.setUpdatedAt(java.time.Instant.now());
        InventoryItem saved = inventoryItemRepository.save(item);

        recordMovement(saved, MovementType.PURCHASE_RECEIPT, request.getQuantity(),
                "GOODS_RECEIPT", request.getReferenceType(), request.getReferenceId(),
                request.getActor() == null || request.getActor().isBlank() ? "SYSTEM" : request.getActor(),
                request.getNotes(), idempotencyKey);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovements(UUID itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));
        return stockMovementRepository.findByItemOrderByMovementTimeDesc(item);
    }

    @Transactional
    public InventoryItem deductSaleStock(UUID itemId, BigDecimal quantity, String actor, String referenceId) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale quantity must be greater than zero.");
        }

        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));

        String idempotencyKey = buildIdempotencyKey("ORDER", referenceId, "SALE");
        if (stockMovementRepository.existsByIdempotencyKey(idempotencyKey)) {
            return item;
        }

        BigDecimal next = item.getCurrentQuantity().subtract(quantity);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Sale exceeds available finished stock for item: " + item.getSku());
        }

        item.setCurrentQuantity(next);
        item.setUpdatedAt(java.time.Instant.now());
        InventoryItem saved = inventoryItemRepository.save(item);

        recordMovement(saved, MovementType.SALE, quantity.negate(),
                "SALE", "ORDER", referenceId,
                actor == null || actor.isBlank() ? "SYSTEM" : actor,
                "Finished goods sale stock deduction.",
                idempotencyKey);

        return saved;
    }

    @Transactional
    public InventoryItem deductSaleStock(SaleStockRequest request) {
        InventoryItem item = getItemBySku(request.getSku());
        return deductSaleStock(item.getId(), request.getQuantity(), request.getActor(), request.getReferenceId());
    }

    private void recordMovement(InventoryItem item, MovementType movementType, BigDecimal quantity,
                                String reason, String referenceType, String referenceId,
                                String actor, String notes, String idempotencyKey) {
        if (idempotencyKey != null && stockMovementRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setReason(reason == null || reason.isBlank() ? "STOCK_ADJUSTMENT" : reason);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setActor(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        movement.setNotes(notes);
        movement.setIdempotencyKey(idempotencyKey);
        movement.setMovementTime(java.time.Instant.now());
        stockMovementRepository.save(movement);
    }

    private MovementType resolveMovementType(String reason) {
        if (reason == null) {
            return MovementType.ADJUSTMENT;
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SALE" -> MovementType.SALE;
            case "PRODUCTION_CONSUMPTION" -> MovementType.PRODUCTION_CONSUMPTION;
            case "PRODUCTION_OUTPUT" -> MovementType.PRODUCTION_OUTPUT;
            case "DAMAGE" -> MovementType.DAMAGE;
            case "LOSS" -> MovementType.LOSS;
            case "PURCHASE_RECEIPT" -> MovementType.PURCHASE_RECEIPT;
            default -> MovementType.ADJUSTMENT;
        };
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private ItemType parseItemType(String value) {
        if (value == null || value.isBlank()) {
            return ItemType.RAW_MATERIAL;
        }
        try {
            return ItemType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported item type: " + value);
        }
    }

    private String buildIdempotencyKey(String referenceType, String referenceId, String reason) {
        String base = String.join("::", referenceType == null ? "" : referenceType,
                referenceId == null ? "" : referenceId,
                reason == null ? "" : reason);
        return base.isBlank() ? UUID.randomUUID().toString() : base;
    }
}
