package com.rrjaggery.customerledger.service;

import com.rrjaggery.customerledger.dto.CustomerPaymentDto;
import com.rrjaggery.customerledger.dto.CustomerStatementDto;
import com.rrjaggery.customerledger.dto.LedgerEntryDto;
import com.rrjaggery.customerledger.dto.RecordPaymentRequest;
import com.rrjaggery.customerledger.entity.*;
import com.rrjaggery.customerledger.repository.CustomerLedgerRepository;
import com.rrjaggery.customerledger.repository.CustomerPaymentRepository;
import com.rrjaggery.customerledger.repository.CustomerRepository;
import com.rrjaggery.customerledger.repository.OfflineOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private static final AtomicInteger PAYMENT_SEQ = new AtomicInteger(500);

    private final CustomerRepository customerRepository;
    private final OfflineOrderRepository offlineOrderRepository;
    private final CustomerLedgerRepository ledgerRepository;
    private final CustomerPaymentRepository paymentRepository;

    public LedgerService(CustomerRepository customerRepository,
                         OfflineOrderRepository offlineOrderRepository,
                         CustomerLedgerRepository ledgerRepository,
                         CustomerPaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.offlineOrderRepository = offlineOrderRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public CustomerPaymentDto recordPayment(UUID customerId, RecordPaymentRequest req, String createdBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        // Idempotency Check
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().trim().isEmpty()) {
            Optional<CustomerPayment> existing = paymentRepository.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                return CustomerPaymentDto.fromEntity(existing.get());
            }
        }

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        if (req.getAmount().compareTo(customer.getCurrentOutstanding()) > 0) {
            throw new IllegalStateException("Payment amount (₹" + req.getAmount() +
                    ") exceeds customer current outstanding balance (₹" + customer.getCurrentOutstanding() + ").");
        }

        OfflineOrder allocatedOrder = null;
        if (req.getOfflineOrderId() != null) {
            allocatedOrder = offlineOrderRepository.findById(req.getOfflineOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Offline order not found with ID: " + req.getOfflineOrderId()));

            if (!allocatedOrder.getCustomer().getId().equals(customerId)) {
                throw new SecurityException("Cannot allocate payment to an order belonging to another customer.");
            }

            if (req.getAmount().compareTo(allocatedOrder.getOutstandingAmount()) > 0) {
                throw new IllegalStateException("Payment amount (₹" + req.getAmount() +
                        ") exceeds invoice outstanding amount (₹" + allocatedOrder.getOutstandingAmount() + ").");
            }

            allocatedOrder.setPaidAmount(allocatedOrder.getPaidAmount().add(req.getAmount()));
            allocatedOrder.setOutstandingAmount(allocatedOrder.getTotalAmount().subtract(allocatedOrder.getPaidAmount()));
            if (allocatedOrder.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
                allocatedOrder.setPaymentStatus("PAID");
            } else {
                allocatedOrder.setPaymentStatus("PARTIALLY_PAID");
            }
            offlineOrderRepository.save(allocatedOrder);
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int seq = PAYMENT_SEQ.incrementAndGet();
        String paymentNumber = String.format("PAY-%s-%04d", dateStr, seq);

        CustomerPayment payment = new CustomerPayment(
                paymentNumber,
                customer,
                allocatedOrder,
                req.getAmount(),
                req.getPaymentMethod(),
                req.getReferenceNotes(),
                req.getIdempotencyKey()
        );
        CustomerPayment savedPayment = paymentRepository.save(payment);

        // Update Ledger
        BigDecimal newBalance = customer.getCurrentOutstanding().subtract(req.getAmount());
        String description = allocatedOrder != null
                ? "Payment received for invoice " + allocatedOrder.getInvoiceNumber() + " via " + req.getPaymentMethod()
                : "Payment received on account via " + req.getPaymentMethod();

        CustomerLedgerEntry creditEntry = new CustomerLedgerEntry(
                customer,
                LedgerTransactionType.CREDIT_PAYMENT,
                "PAYMENT_RECEIPT",
                paymentNumber,
                BigDecimal.ZERO,
                req.getAmount(),
                newBalance,
                null,
                description,
                createdBy != null ? createdBy : "SYSTEM"
        );
        creditEntry.setIdempotencyKey(req.getIdempotencyKey());
        ledgerRepository.save(creditEntry);

        // Update customer running outstanding
        customer.setCurrentOutstanding(newBalance);
        customerRepository.save(customer);

        return CustomerPaymentDto.fromEntity(savedPayment);
    }

    @Transactional(readOnly = true)
    public CustomerStatementDto getCustomerStatement(UUID customerId, Instant from, Instant to) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        List<CustomerLedgerEntry> entries = (from != null && to != null)
                ? ledgerRepository.findCustomerStatement(customerId, from, to)
                : ledgerRepository.findByCustomerIdOrderByTransactionDateAsc(customerId);

        BigDecimal totalDebits = entries.stream()
                .map(CustomerLedgerEntry::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
                .map(CustomerLedgerEntry::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OfflineOrder> overdueOrders = offlineOrderRepository.findOverdueOrdersForCustomer(customerId, Instant.now());
        BigDecimal totalOverdue = overdueOrders.stream()
                .map(OfflineOrder::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerStatementDto statement = new CustomerStatementDto();
        statement.setCustomerId(customer.getId());
        statement.setCustomerName(customer.getContactPerson());
        statement.setBusinessName(customer.getBusinessName());
        statement.setPhone(customer.getPhone());
        statement.setEmail(customer.getEmail());
        statement.setGstin(customer.getGstin());
        statement.setCustomerType(customer.getCustomerType().name());
        statement.setCreditLimit(customer.getCreditLimit());
        statement.setCreditDays(customer.getCreditDays());
        statement.setTotalDebits(totalDebits);
        statement.setTotalCredits(totalCredits);
        statement.setCurrentOutstanding(customer.getCurrentOutstanding());
        statement.setTotalOverdue(totalOverdue);
        statement.setGeneratedAt(Instant.now());
        statement.setEntries(entries.stream().map(LedgerEntryDto::fromEntity).collect(Collectors.toList()));

        return statement;
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentDto> getCustomerPayments(UUID customerId) {
        return paymentRepository.findByCustomerIdOrderByPaymentDateDesc(customerId)
                .stream().map(CustomerPaymentDto::fromEntity).collect(Collectors.toList());
    }
}
