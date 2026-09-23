package com.rrjaggery.customerledger;

import com.rrjaggery.customerledger.dto.*;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.service.CustomerService;
import com.rrjaggery.customerledger.service.OfflineOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    @DisplayName("Should create offline wholesale order with 5% GST and update outstanding balance")
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
        item1.setQuantity(50); // 50 * 100 = 5000 subtotal, 250 tax (5%), 5250 total
        item1.setUnitWeightKg(new BigDecimal("0.5"));

        req.setItems(List.of(item1));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");

        assertNotNull(order.getId());
        assertTrue(order.getOrderNumber().startsWith("OFF-"));
        assertTrue(order.getInvoiceNumber().startsWith("INV-OFF-"));
        assertEquals(new BigDecimal("5000.00"), order.getSubtotalAmount());
        assertEquals(new BigDecimal("250.00"), order.getTaxAmount());
        assertEquals(new BigDecimal("5250.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("5250.00"), order.getOutstandingAmount());
        assertEquals("PENDING", order.getPaymentStatus());

        // Check customer outstanding is updated
        CustomerDto updatedCust = customerService.getCustomerById(customer.getId());
        assertEquals(new BigDecimal("5250.00"), updatedCust.getCurrentOutstanding());
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
        item1.setQuantity(20); // 20 * 500 = 10,000 + 500 tax = 10,500 > 5,000 credit limit!
        item1.setUnitWeightKg(new BigDecimal("10.0"));

        req.setItems(List.of(item1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com"));

        assertTrue(ex.getMessage().contains("Credit limit exceeded"));
    }

    @Test
    @DisplayName("Should generate accurate GST Tax Invoice with CGST (2.5%) and SGST (2.5%)")
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
        item.setQuantity(10); // 2000 subtotal, 100 total tax (50 CGST, 50 SGST)
        item.setUnitWeightKg(new BigDecimal("1.0"));

        req.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");
        OfflineInvoiceDto invoice = offlineOrderService.getOfflineInvoice(order.getId());

        assertNotNull(invoice);
        assertEquals(order.getInvoiceNumber(), invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("2000.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("50.00"), invoice.getCgstAmount());
        assertEquals(new BigDecimal("50.00"), invoice.getSgstAmount());
        assertEquals(new BigDecimal("100.00"), invoice.getTotalTax());
        assertEquals(new BigDecimal("2100.00"), invoice.getGrandTotal());
    }
}
