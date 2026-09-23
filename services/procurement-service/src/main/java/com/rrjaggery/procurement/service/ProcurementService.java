package com.rrjaggery.procurement.service;

import com.rrjaggery.procurement.dto.CreatePurchaseOrderRequest;
import com.rrjaggery.procurement.dto.CreateSupplierRequest;
import com.rrjaggery.procurement.dto.ReceiveGoodsRequest;
import com.rrjaggery.procurement.entity.*;
import com.rrjaggery.procurement.repository.GoodsReceiptRepository;
import com.rrjaggery.procurement.repository.PurchaseOrderRepository;
import com.rrjaggery.procurement.repository.SupplierLedgerEntryRepository;
import com.rrjaggery.procurement.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProcurementService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierLedgerEntryRepository supplierLedgerEntryRepository;
    private final InventoryStockClient inventoryStockClient;

    public ProcurementService(SupplierRepository supplierRepository,
                             PurchaseOrderRepository purchaseOrderRepository,
                             GoodsReceiptRepository goodsReceiptRepository,
                             SupplierLedgerEntryRepository supplierLedgerEntryRepository,
                             InventoryStockClient inventoryStockClient) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierLedgerEntryRepository = supplierLedgerEntryRepository;
        this.inventoryStockClient = inventoryStockClient;
    }

    @Transactional(readOnly = true)
    public List<Supplier> getSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier getSupplier(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    @Transactional(readOnly = true)
    public List<SupplierLedgerEntry> getSupplierLedger(UUID supplierId) {
        Supplier supplier = getSupplier(supplierId);
        return supplierLedgerEntryRepository.findBySupplierOrderByCreatedAtDesc(supplier);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSupplierOutstanding(UUID supplierId) {
        Supplier supplier = getSupplier(supplierId);
        return supplierLedgerEntryRepository.findBySupplierOrderByCreatedAtDesc(supplier).stream()
                .map(entry -> entry.getDebitAmount().subtract(entry.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Supplier createSupplier(CreateSupplierRequest request) {
        String code = normalizeSupplierCode(request.getSupplierCode());
        if (supplierRepository.existsBySupplierCode(code)) {
            throw new IllegalArgumentException("Supplier code already exists: " + code);
        }

        Supplier supplier = new Supplier();
        supplier.setSupplierCode(code);
        supplier.setSupplierName(request.getSupplierName().trim());
        supplier.setContactName(request.getContactName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setGstin(request.getGstin());
        supplier.setPaymentTermsDays(request.getPaymentTermsDays() == null ? 30 : request.getPaymentTermsDays());
        supplier.setStatus(SupplierStatus.ACTIVE);
        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + purchaseOrderId));
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.getSupplierId()));

        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one purchase order line is required.");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setOrderNumber(generateOrderNumber());
        purchaseOrder.setStatus(PurchaseOrderStatus.OPEN);
        purchaseOrder.setNotes(request.getNotes());
        purchaseOrder.setExpectedDeliveryDate(parseDate(request.getExpectedDeliveryDate()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreatePurchaseOrderRequest.PurchaseOrderLineRequest lineRequest : request.getLines()) {
            if (lineRequest.getOrderedQuantity() == null || lineRequest.getOrderedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Ordered quantity must be greater than zero for item " + lineRequest.getSku());
            }
            if (lineRequest.getUnitCost() == null || lineRequest.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit cost must be zero or positive for item " + lineRequest.getSku());
            }

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(purchaseOrder);
            line.setSku(normalizeSku(lineRequest.getSku()));
            line.setItemName(lineRequest.getItemName().trim());
            line.setOrderedQuantity(lineRequest.getOrderedQuantity());
            line.setReceivedQuantity(BigDecimal.ZERO);
            line.setUnitCost(lineRequest.getUnitCost());
            BigDecimal lineTotal = lineRequest.getOrderedQuantity().multiply(lineRequest.getUnitCost());
            line.setLineTotal(lineTotal);

            purchaseOrder.getLines().add(line);
            totalAmount = totalAmount.add(lineTotal);
        }

        purchaseOrder.setTotalAmount(totalAmount);
        purchaseOrder.setTotalReceivedQuantity(BigDecimal.ZERO);
        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public PurchaseOrder receiveGoods(ReceiveGoodsRequest request) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + request.getPurchaseOrderId()));

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Received quantity must be greater than zero.");
        }

        PurchaseOrderLine targetLine = purchaseOrder.getLines().stream()
                .filter(line -> line.getSku().equalsIgnoreCase(normalizeSku(request.getSku())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No line for SKU " + request.getSku() + " on purchase order " + purchaseOrder.getOrderNumber()));

        BigDecimal remainingToReceive = targetLine.getOrderedQuantity().subtract(targetLine.getReceivedQuantity());
        if (request.getQuantity().compareTo(remainingToReceive) > 0) {
            throw new IllegalArgumentException("Receipt exceeds remaining ordered quantity for SKU " + targetLine.getSku());
        }

        long receiptCount = goodsReceiptRepository.findByPurchaseOrderOrderByReceiptTimestampDesc(purchaseOrder).size();
        String idempotencyKey = "po-" + purchaseOrder.getOrderNumber() + "-" + targetLine.getSku() + "-seq-" + (receiptCount + 1);
        if (goodsReceiptRepository.existsByIdempotencyKey(idempotencyKey)) {
            return purchaseOrder;
        }

        targetLine.setReceivedQuantity(targetLine.getReceivedQuantity().add(request.getQuantity()));
        purchaseOrder.setTotalReceivedQuantity(purchaseOrder.getTotalReceivedQuantity().add(request.getQuantity()));

        if (purchaseOrder.getLines().stream().allMatch(line -> line.getReceivedQuantity().compareTo(line.getOrderedQuantity()) >= 0)) {
            purchaseOrder.setStatus(PurchaseOrderStatus.COMPLETED);
        } else {
            purchaseOrder.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setSku(targetLine.getSku());
        receipt.setItemName(targetLine.getItemName());
        receipt.setReceivedQuantity(request.getQuantity());
        receipt.setUnitCost(targetLine.getUnitCost());
        receipt.setReceivedBy(request.getReceivedBy().trim());
        receipt.setNotes(request.getNotes());
        receipt.setIdempotencyKey(idempotencyKey);
        receipt.setReceiptTimestamp(Instant.now());
        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);

        inventoryStockClient.receiveStock(
                targetLine.getSku(),
                request.getQuantity(),
                "PURCHASE_ORDER",
                purchaseOrder.getOrderNumber() + ":" + savedReceipt.getId(),
                request.getReceivedBy(),
                request.getNotes() == null ? "Goods received against purchase order." : request.getNotes()
        );

        BigDecimal receiptValue = request.getQuantity().multiply(targetLine.getUnitCost());
        Supplier supplier = purchaseOrder.getSupplier();
        BigDecimal updatedOutstanding = supplier.getCurrentOutstanding().add(receiptValue);
        supplier.setCurrentOutstanding(updatedOutstanding);
        supplierRepository.save(supplier);

        recordSupplierLedgerEntry(
                supplier,
                LedgerEntryType.INVOICE,
                receiptValue,
                "PO_RECEIPT",
                purchaseOrder.getOrderNumber(),
                request.getReceivedBy(),
                "Goods received against purchase order " + purchaseOrder.getOrderNumber()
        );

        purchaseOrderRepository.save(purchaseOrder);
        return purchaseOrder;
    }

    @Transactional(readOnly = true)
    public List<GoodsReceipt> getReceipts(UUID purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + purchaseOrderId));
        return goodsReceiptRepository.findByPurchaseOrderOrderByReceiptTimestampDesc(purchaseOrder);
    }

    private SupplierLedgerEntry recordSupplierLedgerEntry(Supplier supplier, LedgerEntryType entryType,
                                                         BigDecimal amount, String referenceType,
                                                         String referenceId, String actor, String notes) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        SupplierLedgerEntry entry = new SupplierLedgerEntry();
        entry.setSupplier(supplier);
        entry.setEntryType(entryType);
        entry.setAmount(normalizedAmount);
        entry.setDebitAmount(entryType == LedgerEntryType.INVOICE || entryType == LedgerEntryType.DEBIT ? normalizedAmount : BigDecimal.ZERO);
        entry.setCreditAmount(entryType == LedgerEntryType.PAYMENT || entryType == LedgerEntryType.CREDIT ? normalizedAmount : BigDecimal.ZERO);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setActor(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        entry.setNotes(notes);
        BigDecimal runningBalance = supplier.getCurrentOutstanding();
        entry.setRunningBalance(runningBalance);
        return supplierLedgerEntryRepository.save(entry);
    }

    private String generateOrderNumber() {
        return "PO-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String normalizeSupplierCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Supplier code is required.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private Instant parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateValue).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
