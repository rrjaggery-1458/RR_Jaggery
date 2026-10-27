package com.rrjaggery.customerledger;

import com.rrjaggery.customerledger.dto.*;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.entity.StockOutboxOperation;
import com.rrjaggery.customerledger.entity.StockOutboxStatus;
import com.rrjaggery.customerledger.repository.StockOutboxRepository;
import com.rrjaggery.customerledger.service.CustomerService;
import com.rrjaggery.customerledger.service.OfflineInventoryStockClient;
import com.rrjaggery.customerledger.service.OfflineOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OfflineOrderServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OfflineOrderService offlineOrderService;

    @Autowired
    private StockOutboxRepository stockOutboxRepository;

    @MockBean
    private OfflineInventoryStockClient offlineInventoryStockClient;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.when(offlineInventoryStockClient.checkAvailableStock(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new BigDecimal("500.000"));
    }

    private CustomerDto createTestCustomer(BigDecimal creditLimit, int creditDays) {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        req.setBusinessName("Test Trader " + UUID.randomUUID());
        req.setContactPerson("Trader One");
        req.setPhone("+91 91" + (int)(Math.random() * 89999999 + 10000000));
        req.setPaymentTerms(PaymentTerms.NET_30);
        req.setCreditLimit(creditLimit);
        req.setCreditDays(creditDays);
        return customerService.createCustomer(req, "admin@rrjaggery.com");
    }

    @Test
    @DisplayName("Should create a commercial wholesale order without adding tax and update outstanding balance")
    void testCreateOfflineOrderSuccess() {
        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);

        CreateOfflineOrderRequest req = new CreateOfflineOrderRequest();
        req.setCustomerId(customer.getId());
        req.setPaymentMethod("CREDIT");
        req.setImmediatePaidAmount(BigDecimal.ZERO);
        req.setNotes("Bulk shipment of Jaggery Powder");

        OfflineOrderItemRequest item1 = new OfflineOrderItemRequest();
        item1.setProductId(UUID.randomUUID());
        item1.setProductName("Organic Jaggery Powder 500g");
        item1.setSku("RR-JAG-PWD-500G");
        item1.setUnitPrice(new BigDecimal("100.00"));
        item1.setQuantity(50); // 50 * 100 = 5000 commercial subtotal
        item1.setUnitWeightKg(new BigDecimal("0.5"));

        req.setItems(List.of(item1));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");

        assertNotNull(order.getId());
        assertTrue(order.getOrderNumber().startsWith("OFF-"));
        assertTrue(order.getInvoiceNumber().startsWith("INV-OFF-"));
        assertEquals(new BigDecimal("5000.00"), order.getSubtotalAmount());
        assertEquals(new BigDecimal("0.00"), order.getTaxAmount());
        assertEquals(new BigDecimal("5000.00"), order.getTotalAmount()); // subtotal only
        assertEquals(new BigDecimal("5000.00"), order.getOutstandingAmount());
        assertEquals("PENDING", order.getPaymentStatus());

        // Check customer outstanding is updated
        CustomerDto updatedCust = customerService.getCustomerById(customer.getId());
        assertEquals(new BigDecimal("5000.00"), updatedCust.getCurrentOutstanding());
    }

    @Test
    @DisplayName("Should enforce customer credit limit and reject order exceeding limit")
    void testCreditLimitExceededRejection() {
        CustomerDto customer = createTestCustomer(new BigDecimal("5000.00"), 15);

        CreateOfflineOrderRequest req = new CreateOfflineOrderRequest();
        req.setCustomerId(customer.getId());
        req.setPaymentMethod("CREDIT");
        req.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item1 = new OfflineOrderItemRequest();
        item1.setProductId(UUID.randomUUID());
        item1.setProductName("Organic Jaggery Blocks 10kg");
        item1.setSku("RR-JAG-BLK-10KG");
        item1.setUnitPrice(new BigDecimal("500.00"));
        item1.setQuantity(11); // 11 * 500 = 5500 > 5000 credit limit
        item1.setUnitWeightKg(new BigDecimal("10.0"));

        req.setItems(List.of(item1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com"));

        assertTrue(ex.getMessage().contains("Credit limit exceeded"));
    }

    @Test
    @DisplayName("Should generate a commercial invoice using stored zero-tax values")
    void testGetOfflineInvoice() {
        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);

        CreateOfflineOrderRequest req = new CreateOfflineOrderRequest();
        req.setCustomerId(customer.getId());
        req.setPaymentMethod("CREDIT");
        req.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Natural Liquid Jaggery 1L");
        item.setSku("RR-JAG-LIQ-1L");
        item.setUnitPrice(new BigDecimal("200.00"));
        item.setQuantity(10); // 2000 commercial subtotal
        item.setUnitWeightKg(new BigDecimal("1.0"));

        req.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");
        OfflineInvoiceDto invoice = offlineOrderService.getOfflineInvoice(order.getId());

        assertNotNull(invoice);
        assertEquals(order.getInvoiceNumber(), invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("2000.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("0.00"), invoice.getCgstAmount());
        assertEquals(new BigDecimal("0.00"), invoice.getSgstAmount());
        assertEquals(new BigDecimal("0.00"), invoice.getTotalTax());
        assertEquals(new BigDecimal("2000.00"), invoice.getGrandTotal()); // no tax added
    }

    @Test
    @DisplayName("A delivered 100 KG wholesale order creates a 100 KG stock deduction")
    void deliveredWholesaleOrderEnqueuesKilogramDeduction() {
        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);
        CreateOfflineOrderRequest request = new CreateOfflineOrderRequest();
        request.setCustomerId(customer.getId());
        request.setOrderType("WHOLESALE");
        request.setOrderStatus("DELIVERED");
        request.setPaymentMethod("CASH");
        request.setImmediatePaidAmount(new BigDecimal("14000.00"));

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductName("Jaggery bulk");
        item.setSku("RR-JAG-BULK-1KG");
        item.setUnitPrice(new BigDecimal("140.00"));
        item.setQuantity(100);
        item.setUnitWeightKg(BigDecimal.ONE);
        request.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(request, "admin@rrjaggery.com");
        var stockCommand = stockOutboxRepository.findAll().stream()
                .filter(command -> command.getReferenceId().startsWith(order.getOrderNumber() + ":"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("14000.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("100.000"), stockCommand.getQuantity());
        assertEquals(StockOutboxOperation.DEDUCT, stockCommand.getOperation());
        assertEquals(StockOutboxStatus.PENDING, stockCommand.getStatus());
    }

    @Test
    @DisplayName("A delivered retail order includes delivery but no GST and deducts the sold kilograms")
    void deliveredRetailOrderTotalsAndStockUseKilograms() {
        org.mockito.Mockito.when(offlineInventoryStockClient.checkAvailableStock(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new BigDecimal("100.000"));

        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);
        CreateOfflineOrderRequest request = new CreateOfflineOrderRequest();
        request.setCustomerId(customer.getId());
        request.setOrderType("MANUAL_RETAIL");
        request.setOrderStatus("DELIVERED");
        request.setDeliveryCharges(new BigDecimal("40.00"));
        request.setPaymentMethod("CASH");
        request.setImmediatePaidAmount(new BigDecimal("3240.00"));

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductName("Jaggery retail");
        item.setSku("RR-JAG-RETAIL-1KG");
        item.setUnitPrice(new BigDecimal("160.00"));
        item.setQuantity(20);
        item.setUnitWeightKg(BigDecimal.ONE);
        request.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(request, "admin@rrjaggery.com");
        var stockCommand = stockOutboxRepository.findAll().stream()
                .filter(command -> command.getReferenceId().startsWith(order.getOrderNumber() + ":"))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("3200.00"), order.getSubtotalAmount());
        assertEquals(new BigDecimal("0.00"), order.getTaxAmount());
        assertEquals(new BigDecimal("3240.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("20.000"), stockCommand.getQuantity());
    }

    @Test
    @DisplayName("Reject updating offline order to DELIVERED when stock is insufficient")
    void rejectDeliveredWhenStockInsufficient() {
        org.mockito.Mockito.when(offlineInventoryStockClient.checkAvailableStock(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(BigDecimal.ZERO);

        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);
        CreateOfflineOrderRequest request = new CreateOfflineOrderRequest();
        request.setCustomerId(customer.getId());
        request.setOrderType("WHOLESALE");
        request.setOrderStatus("CONFIRMED");
        request.setPaymentMethod("CREDIT");
        request.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductName("Jaggery insufficient stock");
        item.setSku("RR-JAG-NOSTOCK-1KG");
        item.setUnitPrice(new BigDecimal("140.00"));
        item.setQuantity(10);
        item.setUnitWeightKg(BigDecimal.ONE);
        request.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(request, "admin@rrjaggery.com");

        UpdateOfflineOrderRequest deliveredReq = new UpdateOfflineOrderRequest();
        deliveredReq.setOrderStatus("DELIVERED");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offlineOrderService.updateOfflineOrder(order.getId(), deliveredReq));
        assertTrue(ex.getMessage().contains("The quantity of this product in the order is more than what we have in our stock kindly add production then try again to mark it delivered"));
    }

    @Test
    @DisplayName("Cancelling a dispatched offline order queues restoration after its deduction")
    void dispatchedOfflineOrderCancellationDependsOnSaleCommand() {
        org.mockito.Mockito.when(offlineInventoryStockClient.checkAvailableStock(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new BigDecimal("100.000"));

        CustomerDto customer = createTestCustomer(new BigDecimal("100000.00"), 30);
        CreateOfflineOrderRequest request = new CreateOfflineOrderRequest();
        request.setCustomerId(customer.getId());
        request.setOrderType("WHOLESALE");
        request.setOrderStatus("CONFIRMED");
        request.setPaymentMethod("CREDIT");
        request.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductName("Jaggery cancellation test");
        item.setSku("RR-JAG-CANCEL-1KG");
        item.setUnitPrice(new BigDecimal("140.00"));
        item.setQuantity(10);
        item.setUnitWeightKg(BigDecimal.ONE);
        request.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(request, "admin@rrjaggery.com");

        // Transition to DISPATCHED (deducts stock)
        UpdateOfflineOrderRequest dispatch = new UpdateOfflineOrderRequest();
        dispatch.setOrderStatus("DISPATCHED");
        offlineOrderService.updateOfflineOrder(order.getId(), dispatch);

        // Roll back to CONFIRMED (restores stock)
        UpdateOfflineOrderRequest rollback = new UpdateOfflineOrderRequest();
        rollback.setOrderStatus("CONFIRMED");
        offlineOrderService.updateOfflineOrder(order.getId(), rollback);

        var commands = stockOutboxRepository.findAll().stream()
                .filter(command -> command.getReferenceId().startsWith(order.getOrderNumber() + ":"))
                .toList();
        assertEquals(2, commands.size());
        var sale = commands.stream().filter(command -> command.getOperation() == StockOutboxOperation.DEDUCT)
                .findFirst().orElseThrow();
        var restoration = commands.stream().filter(command -> command.getOperation() == StockOutboxOperation.RESTORE)
                .findFirst().orElseThrow();
        assertEquals(sale.getCommandKey(), restoration.getDependsOnCommandKey());
        assertEquals(new BigDecimal("10.000"), restoration.getQuantity());
    }
}
