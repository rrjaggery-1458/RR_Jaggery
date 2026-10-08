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
    @DisplayName("Should create offline wholesale order with NO GST (RR Jaggery is not GST registered) and update outstanding balance")
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
        item1.setQuantity(50); // 50 * 100 = 5000 subtotal, 0 tax (not GST registered), 5000 total
        item1.setUnitWeightKg(new BigDecimal("0.5"));

        req.setItems(List.of(item1));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");

        assertNotNull(order.getId());
        assertTrue(order.getOrderNumber().startsWith("OFF-"));
        assertTrue(order.getInvoiceNumber().startsWith("INV-OFF-"));
        assertEquals(new BigDecimal("5000.00"), order.getSubtotalAmount());
        assertEquals(new BigDecimal("0.00"), order.getTaxAmount()); // No GST — RR Jaggery is not registered
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
        item1.setQuantity(11); // 11 * 500 = 5500 > 5000 credit limit (no GST added)
        item1.setUnitWeightKg(new BigDecimal("10.0"));

        req.setItems(List.of(item1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com"));

        assertTrue(ex.getMessage().contains("Credit limit exceeded"));
    }

    @Test
    @DisplayName("Should generate invoice with zero tax (RR Jaggery is not GST registered — no CGST/SGST)")
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
        item.setQuantity(10); // 2000 subtotal, 0 tax (not GST registered), 2000 total
        item.setUnitWeightKg(new BigDecimal("1.0"));

        req.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");
        OfflineInvoiceDto invoice = offlineOrderService.getOfflineInvoice(order.getId());

        assertNotNull(invoice);
        assertEquals(order.getInvoiceNumber(), invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("2000.00"), invoice.getSubtotal());
        // RR Jaggery is NOT GST registered — CGST and SGST must be zero
        assertEquals(new BigDecimal("0.00"), invoice.getCgstAmount());
        assertEquals(new BigDecimal("0.00"), invoice.getSgstAmount());
        assertEquals(new BigDecimal("0.00"), invoice.getTotalTax());
        assertEquals(new BigDecimal("2000.00"), invoice.getGrandTotal()); // no tax added
    }
}
