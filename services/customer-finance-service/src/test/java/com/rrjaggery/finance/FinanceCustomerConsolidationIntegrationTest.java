package com.rrjaggery.finance;

import com.rrjaggery.customerledger.dto.CreateCustomerRequest;
import com.rrjaggery.customerledger.dto.CreateOfflineOrderRequest;
import com.rrjaggery.customerledger.dto.CustomerDto;
import com.rrjaggery.customerledger.dto.OfflineOrderDto;
import com.rrjaggery.customerledger.dto.OfflineOrderItemRequest;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.service.CustomerService;
import com.rrjaggery.customerledger.service.OfflineOrderService;
import com.rrjaggery.finance.service.FinanceOrderServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FinanceCustomerConsolidationIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OfflineOrderService offlineOrderService;

    @Autowired
    private FinanceOrderServiceClient financeOrderServiceClient;

    @Test
    void testFinanceAccessToOfflineOrdersWithoutHttp() {
        // 1. Create wholesale customer via customerService
        CreateCustomerRequest custReq = new CreateCustomerRequest();
        custReq.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        custReq.setBusinessName("Consolidation Wholesale Mart " + UUID.randomUUID());
        custReq.setContactPerson("Ramesh Trader");
        custReq.setPhone("+91 9888877777");
        custReq.setPaymentTerms(PaymentTerms.NET_30);
        custReq.setCreditLimit(new BigDecimal("100000.00"));
        custReq.setCreditDays(30);
        CustomerDto customer = customerService.createCustomer(custReq, "admin@rrjaggery.com");
        assertNotNull(customer.getId());

        // 2. Create offline order
        CreateOfflineOrderRequest req = new CreateOfflineOrderRequest();
        req.setCustomerId(customer.getId());
        req.setPaymentMethod("CREDIT");
        req.setImmediatePaidAmount(BigDecimal.ZERO);
        req.setNotes("Consolidation integration test order");

        OfflineOrderItemRequest item = new OfflineOrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Organic Jaggery Bucket 10kg");
        item.setSku("JAG-BUCKET-10");
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("600.00"));
        item.setUnitWeightKg(new BigDecimal("10.0"));
        req.setItems(List.of(item));

        OfflineOrderDto createdOrder = offlineOrderService.createOfflineOrder(req, "admin@rrjaggery.com");
        assertNotNull(createdOrder.getId());
        assertNotNull(createdOrder.getOrderNumber());

        // 3. Verify FinanceOrderServiceClient retrieves order in-process without network HTTP
        List<FinanceOrderServiceClient.RemoteOfflineOrderDto> orders = financeOrderServiceClient.getOfflineOrders();
        assertNotNull(orders);
        assertFalse(orders.isEmpty());

        boolean found = orders.stream().anyMatch(o -> o.orderNumber.equals(createdOrder.getOrderNumber()));
        assertTrue(found, "Created offline order must be visible to FinanceOrderServiceClient via in-process call");
    }
}
