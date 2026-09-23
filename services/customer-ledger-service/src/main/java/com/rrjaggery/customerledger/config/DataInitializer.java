package com.rrjaggery.customerledger.config;

import com.rrjaggery.customerledger.dto.CreateOfflineOrderRequest;
import com.rrjaggery.customerledger.dto.OfflineOrderItemRequest;
import com.rrjaggery.customerledger.entity.Customer;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import com.rrjaggery.customerledger.repository.CustomerRepository;
import com.rrjaggery.customerledger.service.OfflineOrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OfflineOrderService offlineOrderService;

    public DataInitializer(CustomerRepository customerRepository, OfflineOrderService offlineOrderService) {
        this.customerRepository = customerRepository;
        this.offlineOrderService = offlineOrderService;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            // 1. Registered Wholesale Customer (Matches auth user wholesale@example.com)
            Customer karnatakaSweets = new Customer();
            karnatakaSweets.setCustomerType(CustomerType.REGISTERED_WHOLESALE);
            karnatakaSweets.setBusinessName("Karnataka Sweet Mart Pvt Ltd");
            karnatakaSweets.setContactPerson("Suresh Gowda");
            karnatakaSweets.setPhone("+91 98450 12345");
            karnatakaSweets.setEmail("wholesale@example.com");
            karnatakaSweets.setGstin("29AAAAA0000A1Z5");
            karnatakaSweets.setBillingStreet("12/A, Market Road");
            karnatakaSweets.setBillingCity("Mandya");
            karnatakaSweets.setBillingState("Karnataka");
            karnatakaSweets.setBillingPostalCode("571401");
            karnatakaSweets.setShippingStreet("12/A, Market Road");
            karnatakaSweets.setShippingCity("Mandya");
            karnatakaSweets.setShippingState("Karnataka");
            karnatakaSweets.setShippingPostalCode("571401");
            karnatakaSweets.setPaymentTerms(PaymentTerms.NET_30);
            karnatakaSweets.setCreditLimit(new BigDecimal("100000.00"));
            karnatakaSweets.setCreditDays(30);
            karnatakaSweets.setCurrentOutstanding(BigDecimal.ZERO);
            karnatakaSweets.setActive(true);
            Customer savedKsm = customerRepository.save(karnatakaSweets);

            // 2. Offline Wholesale Customer: Mysuru Organic Sweets
            Customer mysuruSweets = new Customer();
            mysuruSweets.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
            mysuruSweets.setBusinessName("Mysuru Organic Sweets & Condiments");
            mysuruSweets.setContactPerson("Anand Murthy");
            mysuruSweets.setPhone("+91 98800 11223");
            mysuruSweets.setEmail("mysurusweets@gmail.com");
            mysuruSweets.setGstin("29ABCDE1234F1Z5");
            mysuruSweets.setBillingStreet("45, Devaraja Market");
            mysuruSweets.setBillingCity("Mysuru");
            mysuruSweets.setBillingState("Karnataka");
            mysuruSweets.setBillingPostalCode("570001");
            mysuruSweets.setShippingStreet("45, Devaraja Market");
            mysuruSweets.setShippingCity("Mysuru");
            mysuruSweets.setShippingState("Karnataka");
            mysuruSweets.setShippingPostalCode("570001");
            mysuruSweets.setPaymentTerms(PaymentTerms.NET_15);
            mysuruSweets.setCreditLimit(new BigDecimal("50000.00"));
            mysuruSweets.setCreditDays(15);
            mysuruSweets.setCurrentOutstanding(BigDecimal.ZERO);
            mysuruSweets.setActive(true);
            Customer savedMysuru = customerRepository.save(mysuruSweets);

            // 3. Offline Wholesale Customer: Bangalore Ayurvedic Nilaya
            Customer blrAyurveda = new Customer();
            blrAyurveda.setCustomerType(CustomerType.OFFLINE_WHOLESALE);
            blrAyurveda.setBusinessName("Bangalore Ayurvedic Nilaya");
            blrAyurveda.setContactPerson("Dr. Vinay Kumar");
            blrAyurveda.setPhone("+91 98440 33445");
            blrAyurveda.setEmail("blr.ayurveda@gmail.com");
            blrAyurveda.setGstin("29AABCB2233C1Z8");
            blrAyurveda.setBillingStreet("88, Gandhi Bazaar Main Rd");
            blrAyurveda.setBillingCity("Bengaluru");
            blrAyurveda.setBillingState("Karnataka");
            blrAyurveda.setBillingPostalCode("560004");
            blrAyurveda.setShippingStreet("88, Gandhi Bazaar Main Rd");
            blrAyurveda.setShippingCity("Bengaluru");
            blrAyurveda.setShippingState("Karnataka");
            blrAyurveda.setShippingPostalCode("560004");
            blrAyurveda.setPaymentTerms(PaymentTerms.NET_30);
            blrAyurveda.setCreditLimit(new BigDecimal("75000.00"));
            blrAyurveda.setCreditDays(30);
            blrAyurveda.setCurrentOutstanding(BigDecimal.ZERO);
            blrAyurveda.setActive(true);
            customerRepository.save(blrAyurveda);

            // 4. Create an initial offline order for Mysuru Organic Sweets
            CreateOfflineOrderRequest orderReq = new CreateOfflineOrderRequest();
            orderReq.setCustomerId(savedMysuru.getId());
            orderReq.setPaymentMethod("CREDIT");
            orderReq.setImmediatePaidAmount(BigDecimal.ZERO);
            orderReq.setNotes("First wholesale consignment - 50kg Organic Jaggery Blocks");

            OfflineOrderItemRequest item1 = new OfflineOrderItemRequest();
            item1.setProductId(UUID.randomUUID());
            item1.setProductName("Organic Jaggery Blocks 10kg");
            item1.setSku("RR-JAG-BLK-10KG");
            item1.setUnitPrice(new BigDecimal("550.00"));
            item1.setQuantity(5);
            item1.setUnitWeightKg(new BigDecimal("10.0"));

            orderReq.setItems(List.of(item1));

            offlineOrderService.createOfflineOrder(orderReq, "admin@rrjaggery.com");

            System.out.println("Customer Ledger Data Seeder completed: 3 initial business customers & 1 demo order seeded.");
        }
    }
}
