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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    void duplicateOutboxDispatch_doesNotCreateDuplicateStockMovement() {
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
        InventoryItem afterBackorderSale = inventoryService.deductSaleStock(sale);
        assertEquals(new BigDecimal("-1.000"), afterBackorderSale.getCurrentQuantity());
        assertEquals(3, inventoryService.getMovements(item.getId()).size());
    }

    @Test
    void cancelledSaleRestoration_isIdempotent() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("FIN-RESTORE-001");
        request.setItemName("Finished Jaggery for restoration test");
        request.setItemType("FINISHED_GOOD");
        request.setInitialQuantity(new BigDecimal("5"));
        InventoryItem item = inventoryService.createItem(request);

        SaleStockRequest sale = new SaleStockRequest();
        sale.setSku(item.getSku());
        sale.setQuantity(new BigDecimal("2"));
        sale.setReferenceType("ORDER");
        sale.setReferenceId("ORD-CANCEL-TEST:LINE-1");
        sale.setActor("commerce-service");
        inventoryService.deductSaleStock(sale);

        InventoryItem afterFirstRestore = inventoryService.restoreSaleStock(sale);
        InventoryItem afterRepeatedRestore = inventoryService.restoreSaleStock(sale);

        assertEquals(new BigDecimal("5.000"), afterFirstRestore.getCurrentQuantity());
        assertEquals(new BigDecimal("5.000"), afterRepeatedRestore.getCurrentQuantity());
        assertEquals(3, inventoryService.getMovements(item.getId()).size());
    }

    @Test
    void concurrentDuplicateSaleCommand_deductsAndRecordsMovementOnce() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setSku("FIN-CONCURRENT-001");
        request.setItemName("Finished Jaggery for concurrency test");
        request.setItemType("FINISHED_GOOD");
        request.setInitialQuantity(new BigDecimal("5"));
        InventoryItem item = inventoryService.createItem(request);

        SaleStockRequest sale = new SaleStockRequest();
        sale.setSku(item.getSku());
        sale.setQuantity(new BigDecimal("2"));
        sale.setReferenceType("ORDER");
        sale.setReferenceId("ORD-CONCURRENT-TEST:LINE-1");
        sale.setActor("commerce-service");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<InventoryItem> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent sale request.");
                }
                return inventoryService.deductSaleStock(sale);
            });
            Future<InventoryItem> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent sale request.");
                }
                return inventoryService.deductSaleStock(sale);
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        InventoryItem finalItem = inventoryService.getItem(item.getId());
        assertEquals(new BigDecimal("3.000"), finalItem.getCurrentQuantity());
        assertEquals(2, inventoryService.getMovements(item.getId()).size());
    }
}
