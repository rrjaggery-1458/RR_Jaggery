package com.rrjaggery.inventoryprocurement;

import com.rrjaggery.inventory.dto.CreateInventoryItemRequest;
import com.rrjaggery.inventory.dto.SaleStockRequest;
import com.rrjaggery.inventory.dto.StockAdjustmentRequest;
import com.rrjaggery.inventory.entity.InventoryItem;
import com.rrjaggery.inventory.service.InventoryService;
import com.rrjaggery.procurement.dto.CreatePurchaseOrderRequest;
import com.rrjaggery.procurement.dto.CreateSupplierRequest;
import com.rrjaggery.procurement.dto.ReceiveGoodsRequest;
import com.rrjaggery.procurement.entity.PurchaseOrder;
import com.rrjaggery.procurement.entity.PurchaseOrderStatus;
import com.rrjaggery.procurement.entity.Supplier;
import com.rrjaggery.procurement.service.ProcurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InventoryProcurementIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProcurementService procurementService;

    @Test
    void testConsolidatedProcurementToInventoryWorkflow() {
        // 1. Create raw material inventory item
        CreateInventoryItemRequest itemReq = new CreateInventoryItemRequest();
        itemReq.setSku("RAW-CANE-01");
        itemReq.setItemName("High Brix Sugarcane");
        itemReq.setItemType("RAW_MATERIAL");
        itemReq.setInitialQuantity(new BigDecimal("100.000"));
        InventoryItem item = inventoryService.createItem(itemReq);
        assertNotNull(item.getId());
        assertEquals(new BigDecimal("100.000"), item.getCurrentQuantity());

        // 2. Create supplier
        CreateSupplierRequest supReq = new CreateSupplierRequest();
        supReq.setSupplierCode("SUP-MANDYA-01");
        supReq.setSupplierName("Mandya Cane Farmers");
        supReq.setContactName("Siddegowda");
        supReq.setPhone("9876543210");
        Supplier supplier = procurementService.createSupplier(supReq);
        assertNotNull(supplier.getId());

        // 3. Create purchase order
        CreatePurchaseOrderRequest poReq = new CreatePurchaseOrderRequest();
        poReq.setSupplierId(supplier.getId());
        poReq.setExpectedDeliveryDate("2026-11-01");
        CreatePurchaseOrderRequest.PurchaseOrderLineRequest line = new CreatePurchaseOrderRequest.PurchaseOrderLineRequest();
        line.setSku("RAW-CANE-01");
        line.setItemName("High Brix Sugarcane");
        line.setOrderedQuantity(new BigDecimal("500.000"));
        line.setUnitCost(new BigDecimal("35.00"));
        poReq.setLines(List.of(line));
        PurchaseOrder po = procurementService.createPurchaseOrder(poReq);
        assertNotNull(po.getId());

        // 4. In-process Goods receipt (Procurement -> Inventory internal call)
        ReceiveGoodsRequest receiveReq = new ReceiveGoodsRequest();
        receiveReq.setPurchaseOrderId(po.getId());
        receiveReq.setSku("RAW-CANE-01");
        receiveReq.setQuantity(new BigDecimal("200.000"));
        receiveReq.setReceivedBy("gate_officer");
        receiveReq.setNotes("First lot received at weighing bridge");
        PurchaseOrder updatedPo = procurementService.receiveGoods(receiveReq);

        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, updatedPo.getStatus());
        assertEquals(new BigDecimal("200.000"), updatedPo.getTotalReceivedQuantity());

        // 5. Verify Inventory stock was increased in-process without network HTTP call
        InventoryItem updatedItem = inventoryService.getItemBySku("RAW-CANE-01");
        assertEquals(new BigDecimal("300.000"), updatedItem.getCurrentQuantity());

        // 6. Test Stock Adjustment
        StockAdjustmentRequest adjReq = new StockAdjustmentRequest();
        adjReq.setItemId(updatedItem.getId());
        adjReq.setQuantity(new BigDecimal("-50.000"));
        adjReq.setReason("CRUSHING_BATCH_01");
        adjReq.setReferenceType("PRODUCTION");
        adjReq.setReferenceId("BATCH-001");
        adjReq.setActor("crusher_operator");
        InventoryItem postAdjItem = inventoryService.adjustStock(adjReq);
        assertEquals(new BigDecimal("250.000"), postAdjItem.getCurrentQuantity());

        // 7. Test Idempotent Sale Stock Deduction
        SaleStockRequest saleReq = new SaleStockRequest();
        saleReq.setSku("RAW-CANE-01");
        saleReq.setQuantity(new BigDecimal("25.000"));
        saleReq.setReferenceId("ORDER-TEST-001");
        saleReq.setActor("commerce-service");

        InventoryItem postSaleItem = inventoryService.deductSaleStock(saleReq);
        assertEquals(new BigDecimal("225.000"), postSaleItem.getCurrentQuantity());

        // Duplicate call with same referenceId should be idempotent
        InventoryItem duplicateSaleItem = inventoryService.deductSaleStock(saleReq);
        assertEquals(new BigDecimal("225.000"), duplicateSaleItem.getCurrentQuantity());
    }
}
