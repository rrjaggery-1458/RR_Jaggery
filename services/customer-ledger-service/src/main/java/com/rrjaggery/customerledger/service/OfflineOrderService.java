package com.rrjaggery.customerledger.service;

import com.rrjaggery.customerledger.dto.*;
import com.rrjaggery.customerledger.entity.*;
import com.rrjaggery.customerledger.repository.CustomerLedgerRepository;
import com.rrjaggery.customerledger.repository.CustomerPaymentRepository;
import com.rrjaggery.customerledger.repository.CustomerRepository;
import com.rrjaggery.customerledger.repository.OfflineOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OfflineOrderService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% GST
    private static final BigDecimal HALF_GST_RATE = new BigDecimal("0.025"); // 2.5% CGST + 2.5% SGST
    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(100);

    private final CustomerRepository customerRepository;
    private final OfflineOrderRepository offlineOrderRepository;
    private final CustomerLedgerRepository ledgerRepository;
    private final CustomerPaymentRepository paymentRepository;

    public OfflineOrderService(CustomerRepository customerRepository,
                               OfflineOrderRepository offlineOrderRepository,
                               CustomerLedgerRepository ledgerRepository,
                               CustomerPaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.offlineOrderRepository = offlineOrderRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OfflineOrderDto createOfflineOrder(CreateOfflineOrderRequest req, String createdBy) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + req.getCustomerId()));

        if (!customer.isActive()) {
            throw new IllegalStateException("Cannot create order for inactive customer: " + customer.getContactPerson());
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        // Calculate Subtotal and Tax
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        OfflineOrder order = new OfflineOrder();
        order.setCustomer(customer);

        for (OfflineOrderItemRequest itemReq : req.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for product: " + itemReq.getProductName());
            }
            if (itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Unit price must be positive for product: " + itemReq.getProductName());
            }

            BigDecimal lineSubtotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            subtotal = subtotal.add(lineSubtotal);
            taxAmount = taxAmount.add(lineTax);

            OfflineOrderItem item = new OfflineOrderItem(
                    itemReq.getProductId(),
                    itemReq.getProductName(),
                    itemReq.getSku(),
                    itemReq.getUnitPrice(),
                    itemReq.getQuantity(),
                    itemReq.getUnitWeightKg(),
                    lineTotal,
                    lineTax
            );
            order.addItem(item);
        }

        BigDecimal totalAmount = subtotal.add(taxAmount);
        BigDecimal immediatePaid = req.getImmediatePaidAmount() != null ? req.getImmediatePaidAmount() : BigDecimal.ZERO;

        if (immediatePaid.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("Immediate payment (₹" + immediatePaid + ") cannot exceed total invoice amount (₹" + totalAmount + ").");
        }

        BigDecimal creditPortion = totalAmount.subtract(immediatePaid);

        // Credit Limit Enforcement
        if (creditPortion.compareTo(BigDecimal.ZERO) > 0 && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal projectedOutstanding = customer.getCurrentOutstanding().add(creditPortion);
            if (projectedOutstanding.compareTo(customer.getCreditLimit()) > 0) {
                throw new IllegalStateException("Credit limit exceeded! Customer credit limit is ₹" + customer.getCreditLimit() +
                        ", current outstanding is ₹" + customer.getCurrentOutstanding() +
                        ", this sale requires ₹" + creditPortion + " credit (exceeds limit by ₹" + projectedOutstanding.subtract(customer.getCreditLimit()) + ").");
            }
        }

        // Sequential IDs
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int seq = ORDER_SEQ.incrementAndGet();
        String orderNumber = String.format("OFF-%s-%04d", dateStr, seq);
        String invoiceNumber = String.format("INV-OFF-%s-%04d", dateStr, seq);

        Instant now = Instant.now();
        Instant dueDate = now.plus(customer.getCreditDays(), ChronoUnit.DAYS);

        order.setOrderNumber(orderNumber);
        order.setInvoiceNumber(invoiceNumber);
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(immediatePaid);
        order.setOutstandingAmount(creditPortion);
        order.setOrderDate(now);
        order.setDueDate(dueDate);
        order.setStatus("CONFIRMED");
        order.setPaymentMethod(req.getPaymentMethod());
        order.setNotes(req.getNotes());

        if (creditPortion.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus("PAID");
        } else if (immediatePaid.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus("PARTIALLY_PAID");
        } else {
            order.setPaymentStatus("PENDING");
        }

        OfflineOrder savedOrder = offlineOrderRepository.save(order);

        // 1. Post Debit Sale to Customer Ledger
        BigDecimal runningBalance = customer.getCurrentOutstanding().add(totalAmount);
        CustomerLedgerEntry debitEntry = new CustomerLedgerEntry(
                customer,
                LedgerTransactionType.DEBIT_SALE,
                "OFFLINE_INVOICE",
                invoiceNumber,
                totalAmount,
                BigDecimal.ZERO,
                runningBalance,
                dueDate,
                "B2B Wholesale Sale Invoice: " + invoiceNumber,
                createdBy != null ? createdBy : "SYSTEM"
        );
        ledgerRepository.save(debitEntry);

        // 2. If immediate payment made, post Credit Payment
        if (immediatePaid.compareTo(BigDecimal.ZERO) > 0) {
            runningBalance = runningBalance.subtract(immediatePaid);
            String payNumber = String.format("PAY-%s-%04d", dateStr, seq);
            CustomerPayment payment = new CustomerPayment(
                    payNumber,
                    customer,
                    savedOrder,
                    immediatePaid,
                    req.getPaymentMethod(),
                    "Immediate payment at offline checkout for " + invoiceNumber,
                    req.getIdempotencyKey()
            );
            paymentRepository.save(payment);

            CustomerLedgerEntry creditEntry = new CustomerLedgerEntry(
                    customer,
                    LedgerTransactionType.CREDIT_PAYMENT,
                    "PAYMENT_RECEIPT",
                    payNumber,
                    BigDecimal.ZERO,
                    immediatePaid,
                    runningBalance,
                    null,
                    "Immediate payment for invoice " + invoiceNumber,
                    createdBy != null ? createdBy : "SYSTEM"
            );
            ledgerRepository.save(creditEntry);
        }

        // Update customer running outstanding
        customer.setCurrentOutstanding(runningBalance);
        customerRepository.save(customer);

        return OfflineOrderDto.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OfflineOrderDto> getCustomerOfflineOrders(UUID customerId) {
        return offlineOrderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream().map(OfflineOrderDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfflineOrderDto> getAllOfflineOrders() {
        return offlineOrderRepository.findAllByOrderByOrderDateDesc()
                .stream().map(OfflineOrderDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfflineOrderDto getOfflineOrderById(UUID orderId) {
        OfflineOrder order = offlineOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Offline order not found with ID: " + orderId));
        return OfflineOrderDto.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OfflineInvoiceDto getOfflineInvoice(UUID orderId) {
        OfflineOrder order = offlineOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Offline order not found with ID: " + orderId));

        Customer customer = order.getCustomer();

        OfflineInvoiceDto invoice = new OfflineInvoiceDto();
        invoice.setInvoiceNumber(order.getInvoiceNumber());
        invoice.setOrderId(order.getId());
        invoice.setOrderNumber(order.getOrderNumber());
        invoice.setInvoiceDate(order.getOrderDate());
        invoice.setDueDate(order.getDueDate());
        invoice.setOverdue(order.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0 && order.getDueDate().isBefore(Instant.now()));

        invoice.setCustomerId(customer.getId());
        invoice.setCustomerName(customer.getContactPerson());
        invoice.setBusinessName(customer.getBusinessName());
        invoice.setCustomerGstin(customer.getGstin());
        invoice.setCustomerPhone(customer.getPhone());
        invoice.setCustomerEmail(customer.getEmail());

        invoice.setBillingAddress(new AddressDto(customer.getBillingStreet(), customer.getBillingCity(), customer.getBillingState(), customer.getBillingPostalCode()));
        invoice.setShippingAddress(new AddressDto(customer.getShippingStreet(), customer.getShippingCity(), customer.getShippingState(), customer.getShippingPostalCode()));

        invoice.setItems(order.getItems().stream().map(OfflineOrderItemDto::fromEntity).collect(Collectors.toList()));
        invoice.setSubtotal(order.getSubtotalAmount());

        BigDecimal cgst = order.getSubtotalAmount().multiply(HALF_GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = order.getSubtotalAmount().multiply(HALF_GST_RATE).setScale(2, RoundingMode.HALF_UP);

        invoice.setCgstAmount(cgst);
        invoice.setSgstAmount(sgst);
        invoice.setTotalTax(order.getTaxAmount());
        invoice.setGrandTotal(order.getTotalAmount());
        invoice.setPaidAmount(order.getPaidAmount());
        invoice.setOutstandingAmount(order.getOutstandingAmount());
        invoice.setPaymentMethod(order.getPaymentMethod());
        invoice.setPaymentStatus(order.getPaymentStatus());

        return invoice;
    }
}
