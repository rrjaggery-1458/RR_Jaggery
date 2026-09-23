package com.rrjaggery.customerledger;

import com.rrjaggery.customerledger.dto.*;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.service.CustomerService;
import com.rrjaggery.customerledger.service.LedgerService;
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
class LedgerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OfflineOrderService offlineOrderService;

    @Autowired
    private LedgerService ledgerService;

    private CustomerDto createTestCustomer() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        req.setBusinessName("Ledger Test Trader " + UUID.randomUUID());
        req.setContactPerson("Trader Two");
        req.setPhone("+91 92" + (int)(Math.random() * 89999999 + 10000000));
        req.setPaymentTerms(PaymentTerms.NET_30);
        req.setCreditLimit(new BigDecimal("500000.00"));
        req.setCreditDays(30);
        return customerService.createCustomer(req, "admin@rrjaggery.com");
    }

    @Test
    @DisplayName("Should record payment, reduce outstanding balance, and generate accurate statement")
    void testRecordPaymentAndStatement() {
        CustomerDto customer = createTestCustomer();

        // 1. Create order for 10 units @ 1000 = 10,000 + 500 tax = 10,500
        CreateOfflineOrderRequest orderReq = new CreateOfflineOrderRequest();
        orderReq.setCustomerId(customer.getId());
        orderReq.setPaymentMethod("CREDIT");
        orderReq.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Organic Jaggery Granules 1kg");
        item.setSku("RR-JAG-GRN-1KG");
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setQuantity(10);
        item.setUnitWeightKg(new BigDecimal("1.0"));

        orderReq.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(orderReq, "admin@rrjaggery.com");

        // Verify initial ledger balance is ₹10,500
        CustomerStatementDto initialStatement = ledgerService.getCustomerStatement(customer.getId(), null, null);
        assertEquals(new BigDecimal("10500.00"), initialStatement.getCurrentOutstanding());
        assertEquals(new BigDecimal("10500.00"), initialStatement.getTotalDebits());
        assertEquals(BigDecimal.ZERO, initialStatement.getTotalCredits());
        assertEquals(1, initialStatement.getEntries().size());

        // 2. Make Partial Payment of ₹5,000 against this order
        RecordPaymentRequest pay1 = new RecordPaymentRequest();
        pay1.setOfflineOrderId(order.getId());
        pay1.setAmount(new BigDecimal("5000.00"));
        pay1.setPaymentMethod("BANK_TRANSFER");
        pay1.setReferenceNotes("NEFT Ref: NEFT123456");

        CustomerPaymentDto payment1 = ledgerService.recordPayment(customer.getId(), pay1, "admin@rrjaggery.com");
        assertNotNull(payment1.getPaymentNumber());
        assertEquals(new BigDecimal("5000.00"), payment1.getAmount());

        // Verify balance is now ₹5,500
        CustomerStatementDto midStatement = ledgerService.getCustomerStatement(customer.getId(), null, null);
        assertEquals(new BigDecimal("5500.00"), midStatement.getCurrentOutstanding());
        assertEquals(new BigDecimal("10500.00"), midStatement.getTotalDebits());
        assertEquals(new BigDecimal("5000.00"), midStatement.getTotalCredits());
        assertEquals(2, midStatement.getEntries().size());

        // 3. Make Final Payment of ₹5,500 to clear entire balance to ₹0
        RecordPaymentRequest pay2 = new RecordPaymentRequest();
        pay2.setOfflineOrderId(order.getId());
        pay2.setAmount(new BigDecimal("5500.00"));
        pay2.setPaymentMethod("UPI");
        pay2.setReferenceNotes("UPI Ref: 9876543210@upi");

        CustomerPaymentDto payment2 = ledgerService.recordPayment(customer.getId(), pay2, "admin@rrjaggery.com");
        assertNotNull(payment2.getPaymentNumber());

        // Verify balance is ₹0
        CustomerStatementDto finalStatement = ledgerService.getCustomerStatement(customer.getId(), null, null);
        assertEquals(new BigDecimal("0.00"), finalStatement.getCurrentOutstanding());
        assertEquals(new BigDecimal("10500.00"), finalStatement.getTotalDebits());
        assertEquals(new BigDecimal("10500.00"), finalStatement.getTotalCredits());
        assertEquals(3, finalStatement.getEntries().size());

        // Verify order status is PAID
        OfflineOrderDto refreshedOrder = offlineOrderService.getOfflineOrderById(order.getId());
        assertEquals("PAID", refreshedOrder.getPaymentStatus());
        assertEquals(new BigDecimal("0.00"), refreshedOrder.getOutstandingAmount());
    }

    @Test
    @DisplayName("Should prevent overpayment exceeding outstanding balance")
    void testOverpaymentRejection() {
        CustomerDto customer = createTestCustomer();

        CreateOfflineOrderRequest orderReq = new CreateOfflineOrderRequest();
        orderReq.setCustomerId(customer.getId());
        orderReq.setPaymentMethod("CREDIT");
        orderReq.setImmediatePaidAmount(BigDecimal.ZERO);

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Organic Jaggery");
        item.setSku("RR-JAG-1KG");
        item.setUnitPrice(new BigDecimal("500.00"));
        item.setQuantity(2); // 1000 + 50 = 1050
        item.setUnitWeightKg(new BigDecimal("1.0"));

        orderReq.setItems(List.of(item));

        OfflineOrderDto order = offlineOrderService.createOfflineOrder(orderReq, "admin@rrjaggery.com");

        RecordPaymentRequest overpayReq = new RecordPaymentRequest();
        overpayReq.setOfflineOrderId(order.getId());
        overpayReq.setAmount(new BigDecimal("2000.00")); // > 1050
        overpayReq.setPaymentMethod("CASH");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ledgerService.recordPayment(customer.getId(), overpayReq, "admin@rrjaggery.com"));

        assertTrue(ex.getMessage().contains("exceeds"));
    }
}
