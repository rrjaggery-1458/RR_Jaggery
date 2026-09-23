package com.rrjaggery.customerledger.service;

import com.rrjaggery.customerledger.dto.AddressDto;
import com.rrjaggery.customerledger.dto.CreateCustomerRequest;
import com.rrjaggery.customerledger.dto.CustomerDto;
import com.rrjaggery.customerledger.dto.UpdateCustomerRequest;
import com.rrjaggery.customerledger.entity.Customer;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.OfflineOrder;
import com.rrjaggery.customerledger.repository.CustomerLedgerRepository;
import com.rrjaggery.customerledger.repository.CustomerRepository;
import com.rrjaggery.customerledger.repository.OfflineOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OfflineOrderRepository offlineOrderRepository;
    private final CustomerLedgerRepository ledgerRepository;

    public CustomerService(CustomerRepository customerRepository,
                           OfflineOrderRepository offlineOrderRepository,
                           CustomerLedgerRepository ledgerRepository) {
        this.customerRepository = customerRepository;
        this.offlineOrderRepository = offlineOrderRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest req, String createdBy) {
        // Check uniqueness for phone
        Optional<Customer> existingPhone = customerRepository.findByPhone(req.getPhone());
        if (existingPhone.isPresent()) {
            throw new IllegalArgumentException("A customer with phone number " + req.getPhone() + " already exists.");
        }

        Customer customer = new Customer();
        customer.setCustomerType(req.getCustomerType());
        customer.setBusinessName(req.getBusinessName());
        customer.setContactPerson(req.getContactPerson());
        customer.setPhone(req.getPhone());
        customer.setEmail(req.getEmail());
        customer.setGstin(req.getGstin());

        if (req.getBillingAddress() != null) {
            customer.setBillingStreet(req.getBillingAddress().getStreet());
            customer.setBillingCity(req.getBillingAddress().getCity());
            customer.setBillingState(req.getBillingAddress().getState());
            customer.setBillingPostalCode(req.getBillingAddress().getPostalCode());
        }

        if (req.getShippingAddress() != null) {
            customer.setShippingStreet(req.getShippingAddress().getStreet());
            customer.setShippingCity(req.getShippingAddress().getCity());
            customer.setShippingState(req.getShippingAddress().getState());
            customer.setShippingPostalCode(req.getShippingAddress().getPostalCode());
        }

        customer.setPaymentTerms(req.getPaymentTerms());
        customer.setCreditLimit(req.getCreditLimit() != null ? req.getCreditLimit() : BigDecimal.ZERO);
        customer.setCreditDays(req.getCreditDays());
        customer.setCurrentOutstanding(BigDecimal.ZERO);
        customer.setActive(true);
        customer.setAuthUserId(req.getAuthUserId());

        Customer saved = customerRepository.save(customer);
        return CustomerDto.fromEntity(saved, BigDecimal.ZERO);
    }

    @Transactional
    public CustomerDto updateCustomer(UUID customerId, UpdateCustomerRequest req) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        if (req.getBusinessName() != null) customer.setBusinessName(req.getBusinessName());
        if (req.getContactPerson() != null) customer.setContactPerson(req.getContactPerson());
        if (req.getPhone() != null) customer.setPhone(req.getPhone());
        if (req.getEmail() != null) customer.setEmail(req.getEmail());
        if (req.getGstin() != null) customer.setGstin(req.getGstin());

        if (req.getBillingAddress() != null) {
            customer.setBillingStreet(req.getBillingAddress().getStreet());
            customer.setBillingCity(req.getBillingAddress().getCity());
            customer.setBillingState(req.getBillingAddress().getState());
            customer.setBillingPostalCode(req.getBillingAddress().getPostalCode());
        }

        if (req.getShippingAddress() != null) {
            customer.setShippingStreet(req.getShippingAddress().getStreet());
            customer.setShippingCity(req.getShippingAddress().getCity());
            customer.setShippingState(req.getShippingAddress().getState());
            customer.setShippingPostalCode(req.getShippingAddress().getPostalCode());
        }

        if (req.getPaymentTerms() != null) customer.setPaymentTerms(req.getPaymentTerms());
        if (req.getCreditLimit() != null) customer.setCreditLimit(req.getCreditLimit());
        if (req.getCreditDays() != null) customer.setCreditDays(req.getCreditDays());
        if (req.getActive() != null) customer.setActive(req.getActive());

        Customer saved = customerRepository.save(customer);
        BigDecimal overdue = calculateCustomerOverdue(saved.getId());
        return CustomerDto.fromEntity(saved, overdue);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        BigDecimal overdue = calculateCustomerOverdue(customerId);
        return CustomerDto.fromEntity(customer, overdue);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerByAuthUserId(UUID authUserId) {
        Customer customer = customerRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found for authenticated user."));
        BigDecimal overdue = calculateCustomerOverdue(customer.getId());
        return CustomerDto.fromEntity(customer, overdue);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers(CustomerType type, String query) {
        List<Customer> customers;
        if (query != null && !query.trim().isEmpty()) {
            customers = customerRepository.searchCustomers(query.trim());
            if (type != null) {
                customers = customers.stream().filter(c -> c.getCustomerType() == type).collect(Collectors.toList());
            }
        } else if (type != null) {
            customers = customerRepository.findByCustomerTypeOrderByCreatedAtDesc(type);
        } else {
            customers = customerRepository.findAllByOrderByCreatedAtDesc();
        }

        return customers.stream().map(c -> {
            BigDecimal overdue = calculateCustomerOverdue(c.getId());
            return CustomerDto.fromEntity(c, overdue);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCustomerOverdue(UUID customerId) {
        List<OfflineOrder> overdueOrders = offlineOrderRepository.findOverdueOrdersForCustomer(customerId, Instant.now());
        return overdueOrders.stream()
                .map(OfflineOrder::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
