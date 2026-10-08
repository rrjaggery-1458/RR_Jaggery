package com.rrjaggery.procurement;

import com.rrjaggery.inventory.dto.ReceiveStockRequest;
import com.rrjaggery.inventory.service.InventoryService;
import com.rrjaggery.procurement.dto.CreatePurchaseOrderRequest;
import com.rrjaggery.procurement.dto.CreateSupplierRequest;
import com.rrjaggery.procurement.dto.ReceiveGoodsRequest;
import com.rrjaggery.procurement.entity.PurchaseOrder;
import com.rrjaggery.procurement.entity.PurchaseOrderStatus;
import com.rrjaggery.procurement.entity.Supplier;
import com.rrjaggery.procurement.repository.GoodsReceiptRepository;
import com.rrjaggery.procurement.repository.SupplierLedgerEntryRepository;
import com.rrjaggery.procurement.service.ProcurementService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ProcurementServiceTest {

    @Autowired
    private ProcurementService procurementService;

    @Autowired
    private GoodsReceiptRepository goodsReceiptRepository;

    @Autowired
    private SupplierLedgerEntryRepository supplierLedgerEntryRepository;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void receiveGoods_recordsReceiptAndCallsInventoryService() {
        CreateSupplierRequest supplierRequest = new CreateSupplierRequest();
        supplierRequest.setSupplierCode("SUP-01");
        supplierRequest.setSupplierName("Mysore Sugars");
        supplierRequest.setContactName("Ravi");
        supplierRequest.setPhone("9999999999");

        Supplier supplier = procurementService.createSupplier(supplierRequest);

        CreatePurchaseOrderRequest purchaseOrderRequest = new CreatePurchaseOrderRequest();
        purchaseOrderRequest.setSupplierId(supplier.getId());
        purchaseOrderRequest.setExpectedDeliveryDate("2025-12-31");
        CreatePurchaseOrderRequest.PurchaseOrderLineRequest line = new CreatePurchaseOrderRequest.PurchaseOrderLineRequest();
        line.setSku("SUGA-001");
        line.setItemName("Sugarcane");
        line.setOrderedQuantity(new BigDecimal("50"));
        line.setUnitCost(new BigDecimal("12"));
        purchaseOrderRequest.setLines(java.util.List.of(line));

        PurchaseOrder purchaseOrder = procurementService.createPurchaseOrder(purchaseOrderRequest);

        ReceiveGoodsRequest receiveRequest = new ReceiveGoodsRequest();
        receiveRequest.setPurchaseOrderId(purchaseOrder.getId());
        receiveRequest.setSku("SUGA-001");
        receiveRequest.setQuantity(new BigDecimal("20"));
        receiveRequest.setReceivedBy("warehouse");
        receiveRequest.setNotes("Received sugarcane");

        PurchaseOrder updated = procurementService.receiveGoods(receiveRequest);

        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, updated.getStatus());
        assertEquals(1, goodsReceiptRepository.findByPurchaseOrderOrderByReceiptTimestampDesc(updated).size());
        assertEquals(new BigDecimal("240.00"), procurementService.getSupplierOutstanding(supplier.getId()));
        assertEquals(1, supplierLedgerEntryRepository.findBySupplierOrderByCreatedAtDesc(supplier).size());

        ArgumentCaptor<ReceiveStockRequest> captor = ArgumentCaptor.forClass(ReceiveStockRequest.class);
        verify(inventoryService, times(1)).receiveStock(captor.capture());
        ReceiveStockRequest captured = captor.getValue();
        assertEquals("SUGA-001", captured.getSku());
        assertEquals(new BigDecimal("20"), captured.getQuantity());
        assertEquals("PURCHASE_ORDER", captured.getReferenceType());
        assertEquals("warehouse", captured.getActor());
        assertNotNull(captured.getReferenceId());
    }
}
