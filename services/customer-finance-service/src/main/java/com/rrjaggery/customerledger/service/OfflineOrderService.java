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

    // Commercial wholesale/offline orders currently store item prices without GST.
    // Tax fields are retained in DB schema for backward compatibility only.
    private static final BigDecimal GST_RATE = BigDecimal.ZERO;
    private static final BigDecimal HALF_GST_RATE = BigDecimal.ZERO;
    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(100);

    private final CustomerRepository customerRepository;
    private final OfflineOrderRepository offlineOrderRepository;
    private final CustomerLedgerRepository ledgerRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final StockOutboxService stockOutboxService;

    public OfflineOrderService(CustomerRepository customerRepository,
                               OfflineOrderRepository offlineOrderRepository,
                               CustomerLedgerRepository ledgerRepository,
                               CustomerPaymentRepository paymentRepository,
                               StockOutboxService stockOutboxService) {
        this.customerRepository = customerRepository;
        this.offlineOrderRepository = offlineOrderRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
        this.stockOutboxService = stockOutboxService;
    }

    @Transactional
    public OfflineOrderDto createOfflineOrder(CreateOfflineOrderRequest req, String createdBy) {
        Customer customer;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + req.getCustomerId()));
        } else if (req.getCustomerName() != null && !req.getCustomerName().isBlank()) {
            String cName = req.getCustomerName().trim();
            customer = customerRepository.findAll().stream()
                    .filter(c -> cName.equalsIgnoreCase(c.getContactPerson()) || cName.equalsIgnoreCase(c.getBusinessName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Customer newCust = new Customer();
                        newCust.setCustomerType(CustomerType.RETAIL);
                        newCust.setBusinessName(cName);
                        newCust.setContactPerson(cName);
                        newCust.setPhone("0000000000");
                        newCust.setCreditLimit(BigDecimal.ZERO);
                        newCust.setCreditDays(0);
                        return customerRepository.save(newCust);
                    });
        } else {
            throw new IllegalArgumentException("Either customerId or customerName is required.");
        }

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

            UUID pId = itemReq.getProductId() != null ? itemReq.getProductId() : UUID.randomUUID();
            String pSku = itemReq.getSku() != null && !itemReq.getSku().isBlank() ? itemReq.getSku() : "MANUAL-SKU";
            BigDecimal uWeight = itemReq.getUnitWeightKg() != null ? itemReq.getUnitWeightKg() : BigDecimal.ONE;

            OfflineOrderItem item = new OfflineOrderItem(
                    pId,
                    itemReq.getProductName(),
                    pSku,
                    itemReq.getUnitPrice(),
                    itemReq.getQuantity(),
                    uWeight,
                    lineTotal,
                    lineTax
            );
            order.addItem(item);
        }

        BigDecimal deliveryCharges = req.getDeliveryCharges() != null ? req.getDeliveryCharges() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryCharges);
        BigDecimal immediatePaid = req.getImmediatePaidAmount() != null ? req.getImmediatePaidAmount() : BigDecimal.ZERO;

        // If payment status is explicitly PENDING/UNPAID/CREDIT, do NOT auto-settle the invoice even if cash/upi was selected as the intended tender
        boolean isPendingPayment = "PENDING".equalsIgnoreCase(req.getPaymentStatus())
                || "UNPAID".equalsIgnoreCase(req.getPaymentStatus())
                || "CREDIT".equalsIgnoreCase(req.getPaymentStatus());

        // If payment status is marked PAID or cash/upi and not explicitly pending, default immediatePaid to totalAmount if not set
        if (!isPendingPayment && ("PAID".equalsIgnoreCase(req.getPaymentStatus()) || "CASH".equalsIgnoreCase(req.getPaymentMethod()) || "UPI".equalsIgnoreCase(req.getPaymentMethod()) || "BANK_TRANSFER".equalsIgnoreCase(req.getPaymentMethod()))) {
            if (immediatePaid.compareTo(BigDecimal.ZERO) == 0 && req.getImmediatePaidAmount() == null) {
                immediatePaid = totalAmount;
            }
        }

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
        String prefix = "MANUAL_RETAIL".equalsIgnoreCase(req.getOrderType()) ? "RET" : "OFF";
        String orderNumber = String.format("%s-%s-%04d", prefix, dateStr, seq);
        String invoiceNumber = String.format("INV-%s-%s-%04d", prefix, dateStr, seq);

        Instant orderDate = req.getOrderDate() != null ? req.getOrderDate() : Instant.now();
        Instant dueDate = orderDate.plus(customer.getCreditDays() > 0 ? customer.getCreditDays() : 30, ChronoUnit.DAYS);

        order.setOrderNumber(orderNumber);
        order.setInvoiceNumber(invoiceNumber);
        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(taxAmount);
        order.setDeliveryCharges(deliveryCharges);
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(immediatePaid);
        order.setOutstandingAmount(creditPortion);
        order.setOrderDate(orderDate);
        order.setDueDate(dueDate);
        order.setOrderType(req.getOrderType() != null ? req.getOrderType().trim().toUpperCase() : "WHOLESALE");
        String initialStatus = req.getOrderStatus() != null && !req.getOrderStatus().isBlank()
                ? req.getOrderStatus().trim().toUpperCase() : "CONFIRMED";
        validateOrderStatus(initialStatus);
        order.setStatus(initialStatus);
        order.setPaymentMethod(req.getPaymentMethod());
        order.setNotes(req.getNotes());

        if (creditPortion.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus("PAID");
        } else if (req.getPaymentStatus() != null && !req.getPaymentStatus().isBlank()) {
            order.setPaymentStatus(req.getPaymentStatus());
        } else if (immediatePaid.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus("PARTIALLY_PAID");
        } else {
            order.setPaymentStatus("PENDING");
        }

        OfflineOrder savedOrder = offlineOrderRepository.save(order);
        if (isStockConsumingStatus(savedOrder.getStatus())) {
            stockOutboxService.enqueueSale(savedOrder);
        }

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

        // Commercial invoice tax values come from the stored order; do not infer tax in this projection.
        BigDecimal cgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

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

    @Transactional
    public OfflineOrderDto updateOfflineOrder(UUID orderId, UpdateOfflineOrderRequest req) {
        OfflineOrder order = offlineOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Offline order not found with ID: " + orderId));

        Customer customer = order.getCustomer();

        String previousStatus = normalizeStatus(order.getStatus());
        String requestedStatus = null;
        if (req.getOrderStatus() != null && !req.getOrderStatus().isBlank()) {
            requestedStatus = req.getOrderStatus().trim().toUpperCase();
            validateOrderStatus(requestedStatus);
            if ("CANCELLED".equals(previousStatus) && !"CANCELLED".equals(requestedStatus)) {
                throw new IllegalStateException("Cancelled orders cannot return to fulfillment.");
            }
            if ("CANCELLED".equals(requestedStatus)
                    && !List.of("PENDING", "CONFIRMED").contains(previousStatus)) {
                throw new IllegalStateException("Orders already in processing or dispatch cannot be cancelled automatically.");
            }
            order.setStatus(requestedStatus);
        }

        if (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()) {
            order.setPaymentMethod(req.getPaymentMethod().trim());
        }

        if (req.getNotes() != null) {
            order.setNotes(req.getNotes().trim());
        }

        if (req.getPaymentStatus() != null && !req.getPaymentStatus().isBlank()) {
            String newPayStatus = req.getPaymentStatus().trim().toUpperCase();
            String oldPayStatus = order.getPaymentStatus();
            order.setPaymentStatus(newPayStatus);

            if ("PAID".equals(newPayStatus) && !"PAID".equalsIgnoreCase(oldPayStatus)) {
                BigDecimal prevOutstanding = order.getOutstandingAmount();
                order.setPaidAmount(order.getTotalAmount());
                order.setOutstandingAmount(BigDecimal.ZERO);
                if (customer != null && prevOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal updatedCustBal = customer.getCurrentOutstanding().subtract(prevOutstanding);
                    if (updatedCustBal.compareTo(BigDecimal.ZERO) < 0) updatedCustBal = BigDecimal.ZERO;
                    customer.setCurrentOutstanding(updatedCustBal);
                    customerRepository.save(customer);
                }
            } else if ("PENDING".equals(newPayStatus) && "PAID".equalsIgnoreCase(oldPayStatus)) {
                order.setPaidAmount(BigDecimal.ZERO);
                order.setOutstandingAmount(order.getTotalAmount());
                if (customer != null) {
                    customer.setCurrentOutstanding(customer.getCurrentOutstanding().add(order.getTotalAmount()));
                    customerRepository.save(customer);
                }
            }
        }

        OfflineOrder saved = offlineOrderRepository.save(order);
        if (requestedStatus != null) {
            if (!isStockConsumingStatus(previousStatus) && isStockConsumingStatus(requestedStatus)) {
                stockOutboxService.enqueueSale(saved);
            } else if (!"CANCELLED".equals(previousStatus) && "CANCELLED".equals(requestedStatus)) {
                stockOutboxService.enqueueRestoration(saved);
            }
        }
        return OfflineOrderDto.fromEntity(saved);
    }

    @Transactional
    public void deleteOfflineOrder(UUID orderId) {
        OfflineOrder order = offlineOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Offline order not found with ID: " + orderId));

        if (isStockConsumingStatus(normalizeStatus(order.getStatus()))) {
            throw new IllegalStateException("Fulfilled orders cannot be deleted; cancel an eligible order to restore stock first.");
        }
        Customer customer = order.getCustomer();
        if (customer != null && order.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal updatedCustBal = customer.getCurrentOutstanding().subtract(order.getOutstandingAmount());
            if (updatedCustBal.compareTo(BigDecimal.ZERO) < 0) updatedCustBal = BigDecimal.ZERO;
            customer.setCurrentOutstanding(updatedCustBal);
            customerRepository.save(customer);
        }

        offlineOrderRepository.delete(order);
    }

    private boolean isStockConsumingStatus(String status) {
        String normalized = normalizeStatus(status);
        return "CONFIRMED".equals(normalized) || "PROCESSING".equals(normalized)
                || "DISPATCHED".equals(normalized) || "DELIVERED".equals(normalized);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private void validateOrderStatus(String status) {
        if (!List.of("PENDING", "CONFIRMED", "PROCESSING", "DISPATCHED", "DELIVERED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Unsupported order status: " + status);
        }
    }
}
