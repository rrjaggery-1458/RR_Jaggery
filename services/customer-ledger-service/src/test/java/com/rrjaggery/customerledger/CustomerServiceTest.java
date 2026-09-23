package com.rrjaggery.customerledger;

import com.rrjaggery.customerledger.dto.AddressDto;
import com.rrjaggery.customerledger.dto.CreateCustomerRequest;
import com.rrjaggery.customerledger.dto.CustomerDto;
import com.rrjaggery.customerledger.dto.UpdateCustomerRequest;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Test
    @DisplayName("Should create offline wholesale customer with credit terms")
    void testCreateOfflineCustomer() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        req.setBusinessName("Cauvery Organic Traders");
        req.setContactPerson("Ramesh Kumar");
        req.setPhone("+91 98800 99887");
        req.setEmail("cauvery@traders.com");
        req.setGstin("29AAACH1234F1Z1");
        req.setPaymentTerms(PaymentTerms.NET_30);
        req.setCreditLimit(new BigDecimal("150000.00"));
        req.setCreditDays(30);
        req.setBillingAddress(new AddressDto("10, Bazaar St", "Mandya", "Karnataka", "571401"));

        CustomerDto created = customerService.createCustomer(req, "admin@rrjaggery.com");

        assertNotNull(created.getId());
        assertEquals("Cauvery Organic Traders", created.getBusinessName());
        assertEquals(CustomerType.OFFLINE_WHOLESALE, created.getCustomerType());
        assertEquals(new BigDecimal("150000.00"), created.getCreditLimit());
        assertEquals(30, created.getCreditDays());
        assertEquals(BigDecimal.ZERO, created.getCurrentOutstanding());
        assertTrue(created.isActive());
    }

    @Test
    @DisplayName("Should prevent duplicate customer creation with existing phone number")
    void testDuplicatePhoneThrowsException() {
        CreateCustomerRequest req1 = new CreateCustomerRequest();
        req1.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        req1.setBusinessName("Unique Phone Test 1");
        req1.setContactPerson("Person 1");
        req1.setPhone("+91 99999 88888");
        req1.setPaymentTerms(PaymentTerms.IMMEDIATE_CASH);
        req1.setCreditLimit(BigDecimal.ZERO);
        req1.setCreditDays(0);

        customerService.createCustomer(req1, "admin@rrjaggery.com");

        CreateCustomerRequest req2 = new CreateCustomerRequest();
        req2.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
        req2.setBusinessName("Unique Phone Test 2");
        req2.setContactPerson("Person 2");
        req2.setPhone("+91 99999 88888");
        req2.setPaymentTerms(PaymentTerms.IMMEDIATE_CASH);
        req2.setCreditLimit(BigDecimal.ZERO);
        req2.setCreditDays(0);

        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer(req2, "admin@rrjaggery.com"));
    }

    @Test
    @DisplayName("Should update customer credit limit and details")
    void testUpdateCustomer() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setCustomerType(CustomerType.REGISTERED_WHOLESALE);
        req.setBusinessName("Veda Sweets");
        req.setContactPerson("Veda Vyas");
        req.setPhone("+91 97777 66666");
        req.setPaymentTerms(PaymentTerms.NET_15);
        req.setCreditLimit(new BigDecimal("50000.00"));
        req.setCreditDays(15);

        CustomerDto created = customerService.createCustomer(req, "admin@rrjaggery.com");

        UpdateCustomerRequest updateReq = new UpdateCustomerRequest();
        updateReq.setCreditLimit(new BigDecimal("80000.00"));
        updateReq.setCreditDays(30);
        updateReq.setPaymentTerms(PaymentTerms.NET_30);

        CustomerDto updated = customerService.updateCustomer(created.getId(), updateReq);

        assertEquals(new BigDecimal("80000.00"), updated.getCreditLimit());
        assertEquals(30, updated.getCreditDays());
        assertEquals(PaymentTerms.NET_30, updated.getPaymentTerms());
    }

    @Test
    @DisplayName("Should search customer by business name or phone")
    void testSearchCustomers() {
        List<CustomerDto> results = customerService.getAllCustomers(null, "Karnataka");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(c -> c.getBusinessName().contains("Karnataka")));
    }
}
