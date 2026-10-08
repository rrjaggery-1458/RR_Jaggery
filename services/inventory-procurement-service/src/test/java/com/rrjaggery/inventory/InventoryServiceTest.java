package com.rrjaggery.inventory;

import com.rrjaggery.inventory.dto.CreateInventoryItemRequest;
import com.rrjaggery.inventory.dto.StockAdjustmentRequest;
import com.rrjaggery.inventory.dto.SaleStockRequest;
import com.rrjaggery.inventory.entity.InventoryItem;
import com.rrjaggery.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void createItem_rejectsNegativeInitialQuantity() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("SUGA-001");
        request.setItemName("Sugarcane");
        request.setItemType("RAW_MATERIAL");
        request.setInitialQuantity(new BigDecimal("-1"));

        assertThrows(IllegalArgumentException.class, () -> inventoryService.createItem(request));
    }

    @Test
    void adjustStock_rejectsNegativeStockBalance() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("SUGA-010");
        request.setItemName("Sugarcane");
        request.setItemType("RAW_MATERIAL");
        request.setInitialQuantity(new BigDecimal("10"));

        InventoryItem item = inventoryService.createItem(request);

        StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
        adjustment.setItemId(item.getId());
        adjustment.setQuantity(new BigDecimal("-15"));
        adjustment.setReason("SALE");
        adjustment.setReferenceType("ORDER");
        adjustment.setReferenceId("ORD-TEST");
        adjustment.setActor("tester");

        assertThrows(IllegalArgumentException.class, () -> inventoryService.adjustStock(adjustment));
    }

    @Test
    void adjustStock_persistsCurrentQuantityAndMovement() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("LIME-001");
        request.setItemName("Lime");
        request.setItemType("RAW_MATERIAL");
        request.setInitialQuantity(new BigDecimal("12"));

        InventoryItem item = inventoryService.createItem(request);

        StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
        adjustment.setItemId(item.getId());
        adjustment.setQuantity(new BigDecimal("-4"));
        adjustment.setReason("PRODUCTION_CONSUMPTION");
        adjustment.setReferenceType("BATCH");
        adjustment.setReferenceId("BATCH-100");
        adjustment.setActor("plant");
        adjustment.setNotes("Used for boiling");

        InventoryItem updated = inventoryService.adjustStock(adjustment);

        assertEquals(new BigDecimal("8.000"), updated.getCurrentQuantity());
        assertEquals(2, inventoryService.getMovements(item.getId()).size());
    }

    @Test
    void saleStock_isIdempotentAndPreventsNegativeStock() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("FIN-001");
        request.setItemName("Finished Jaggery");
        request.setItemType("FINISHED_GOOD");
        request.setInitialQuantity(new BigDecimal("5"));

        InventoryItem item = inventoryService.createItem(request);
        SaleStockRequest sale = new SaleStockRequest();
        sale.setSku("FIN-001");
        sale.setQuantity(new BigDecimal("2"));
        sale.setReferenceId("ORD-TEST:LINE-1");
        sale.setActor("commerce-service");

        InventoryItem afterFirstSale = inventoryService.deductSaleStock(sale);
        InventoryItem afterDuplicateSale = inventoryService.deductSaleStock(sale);

        assertEquals(new BigDecimal("3.000"), afterFirstSale.getCurrentQuantity());
        assertEquals(new BigDecimal("3.000"), afterDuplicateSale.getCurrentQuantity());
        assertEquals(2, inventoryService.getMovements(item.getId()).size());

        sale.setReferenceId("ORD-TEST:LINE-2");
        sale.setQuantity(new BigDecimal("4"));
        assertThrows(IllegalStateException.class, () -> inventoryService.deductSaleStock(sale));
    }
}
