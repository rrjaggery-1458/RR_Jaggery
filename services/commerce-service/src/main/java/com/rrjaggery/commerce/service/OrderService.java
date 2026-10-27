package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.*;
import com.rrjaggery.commerce.entity.*;
import com.rrjaggery.commerce.repository.CartItemRepository;
import com.rrjaggery.commerce.repository.OrderItemRepository;
import com.rrjaggery.commerce.repository.OrderRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    // Commercial orders currently store item prices without GST. Statutory tax handling is separate.
    // Tax fields are retained in DB schema for audit/backward compatibility only.
    private static final BigDecimal GST_RATE = BigDecimal.ZERO;
    private static final BigDecimal HALF_GST_RATE = BigDecimal.ZERO;

    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("50.00");
    private static final BigDecimal STANDARD_PACKAGING_FEE = new BigDecimal("10.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StockOutboxService stockOutboxService;
    private final InventoryStockClient inventoryStockClient;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            StockOutboxService stockOutboxService,
            InventoryStockClient inventoryStockClient
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.stockOutboxService = stockOutboxService;
        this.inventoryStockClient = inventoryStockClient;
    }

    public OrderDto checkout(
            UUID userId,
            String customerName,
            String customerEmail,
            String customerPhone,
            String customerType,
            CheckoutRequest request
    ) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart.");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerPhone(customerPhone != null ? customerPhone : request.getShippingAddress().getPhone());
        order.setCustomerType(customerType != null ? customerType : "RETAIL");

        AddressDto addr = request.getShippingAddress();
        order.setRecipientName(addr.getRecipientName());
        order.setPhone(addr.getPhone());
        order.setApartmentName(addr.getApartmentName());
        order.setStreetAddress(addr.getStreetAddress());
        order.setCity(addr.getCity());
        order.setState(addr.getState());
        order.setPostalCode(addr.getPostalCode());
        order.setNearestLandmark(addr.getNearestLandmark());
        order.setNotes(request.getNotes());

        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.TEST_PAYMENT;
        order.setPaymentMethod(paymentMethod);

        if (paymentMethod == PaymentMethod.TEST_PAYMENT || paymentMethod == PaymentMethod.RAZORPAY) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setStatus(OrderStatus.PENDING);
        }

        String orderNumber = generateOrderNumber();
        order.setOrderNumber(orderNumber);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product currentProduct = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException("Product unavailable: " + cartItem.getProduct().getId()));

            if (!currentProduct.isActive()) {
                throw new IllegalStateException("Product '" + currentProduct.getName() + "' is inactive and cannot be ordered.");
            }

            BigDecimal unitPrice = CartService.resolvePrice(currentProduct, cartItem.getQuantity(), customerType);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineTotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem(
                    currentProduct.getId(),
                    currentProduct.getName(),
                    currentProduct.getSku(),
                    unitPrice,
                    cartItem.getQuantity(),
                    currentProduct.getUnitWeightKg(),
                    lineTotal,
                    lineTax
            );
            order.addItem(orderItem);

            subtotal = subtotal.add(lineTotal);
            totalTax = totalTax.add(lineTax);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        totalTax = totalTax.setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping = STANDARD_SHIPPING_FEE;
        BigDecimal packaging = STANDARD_PACKAGING_FEE;

        BigDecimal grandTotal = subtotal.add(totalTax).add(shipping).add(packaging).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(totalTax);
        order.setShippingAmount(shipping);
        order.setPackagingFee(packaging);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(grandTotal);

        Order saved = orderRepository.save(order);

        if (isStockConsumingStatus(saved.getStatus())) {
            stockOutboxService.enqueueSale(saved);
        }

        // Clear cart atomically
        cartItemRepository.deleteByUserId(userId);

        return OrderDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getCustomerOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID orderId, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not have permission to view this order.");
        }

        return OrderDto.fromEntity(order);
    }

    public OrderDto cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not have permission to cancel this order.");
        }

        if (!isCancellableStatus(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus()
                    + ". Only PENDING or CONFIRMED orders can be cancelled automatically.");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            stockOutboxService.enqueueRestoration(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Order saved = orderRepository.save(order);
        return OrderDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersAdmin(OrderStatus status) {
        List<Order> orders = status != null
                ? orderRepository.findByStatusOrderByCreatedAtDesc(status)
                : orderRepository.findAllByOrderByCreatedAtDesc();

        return orders.stream().map(OrderDto::fromEntity).collect(Collectors.toList());
    }

    public OrderDto updateOrderStatus(UUID orderId, OrderStatus newStatus, PaymentStatus newPaymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (newStatus != null) {
            OrderStatus previousStatus = order.getStatus();
            if (previousStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
                throw new IllegalStateException("Cancelled orders cannot return to fulfillment.");
            }
            if (newStatus == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED
                    && !isCancellableStatus(previousStatus)) {
                throw new IllegalStateException("Orders already in processing or dispatch cannot be cancelled automatically.");
            }
            if (!isStockConsumingStatus(previousStatus) && isStockConsumingStatus(newStatus)) {
                // ── Stock availability pre-check ─────────────────────────────────────
                // Online orders can be placed regardless of stock, but CANNOT be
                // moved to DISPATCHED/DELIVERED when inventory is insufficient.
                // Check live stock before marking dispatched/delivered and enqueuing the sale.
                for (com.rrjaggery.commerce.entity.OrderItem item : order.getItems()) {
                    java.math.BigDecimal weight = item.getUnitWeightKg();
                    if (weight == null || weight.compareTo(java.math.BigDecimal.ZERO) <= 0) weight = java.math.BigDecimal.ONE;
                    java.math.BigDecimal required = java.math.BigDecimal.valueOf(item.getQuantity())
                            .multiply(weight)
                            .setScale(3, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal available = inventoryStockClient.checkAvailableStock(item.getSku());
                    if (available == null) available = java.math.BigDecimal.ZERO;
                    if (available.compareTo(required) < 0) {
                        String targetAction = newStatus == OrderStatus.DELIVERED ? "delivered" : "dispatched";
                        throw new IllegalStateException(
                                "The quantity of this product in the order is more than what we have in our stock kindly add production then try again to mark it "
                                + targetAction + ". (Product: \"" + item.getProductName() + "\", SKU: " + item.getSku()
                                + ", Required: " + required.stripTrailingZeros().toPlainString() + " KG, Available: "
                                + available.stripTrailingZeros().toPlainString() + " KG)");
                    }
                }
                // ─────────────────────────────────────────────────────────────────────
                stockOutboxService.enqueueSale(order);
            }
            if (newStatus == OrderStatus.CANCELLED && isStockConsumingStatus(previousStatus)) {
                stockOutboxService.enqueueRestoration(order);
            }
            if (isStockConsumingStatus(previousStatus) && !isStockConsumingStatus(newStatus) && newStatus != OrderStatus.CANCELLED) {
                stockOutboxService.enqueueRestoration(order);
            }
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING && newPaymentStatus == null) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        if (newPaymentStatus != null) {
            order.setPaymentStatus(newPaymentStatus);
        }

        Order saved = orderRepository.save(order);
        return OrderDto.fromEntity(saved);
    }

    public OrderDto updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        return updateOrderStatus(orderId, newStatus, null);
    }

    public void deleteOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        if (isStockConsumingStatus(order.getStatus())) {
            throw new IllegalStateException("Fulfilled orders cannot be deleted; cancel an eligible order to restore stock first.");
        }
        orderRepository.delete(order);
    }

    private boolean isStockConsumingStatus(OrderStatus status) {
        return status == OrderStatus.DISPATCHED || status == OrderStatus.DELIVERED;
    }

    private boolean isCancellableStatus(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceData(UUID orderId, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not have permission to access this invoice.");
        }

        InvoiceDto invoice = new InvoiceDto();
        invoice.setOrderId(order.getId());
        invoice.setOrderNumber(order.getOrderNumber());
        invoice.setInvoiceNumber("INV-" + order.getOrderNumber().replace("ORD-", ""));
        invoice.setInvoiceDate(order.getCreatedAt());

        invoice.setCustomerName(order.getCustomerName());
        invoice.setCustomerEmail(order.getCustomerEmail());
        invoice.setCustomerPhone(order.getCustomerPhone());
        invoice.setShippingAddress(new AddressDto(
                order.getRecipientName(),
                order.getPhone(),
                order.getApartmentName(),
                order.getStreetAddress(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getNearestLandmark()
        ));

        invoice.setItems(order.getItems().stream().map(OrderItemDto::fromEntity).collect(Collectors.toList()));
        invoice.setSubtotal(order.getSubtotalAmount());

        // Commercial invoice tax values come from the stored order; do not infer tax in this projection.
        BigDecimal cgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        invoice.setCgstAmount(cgst);
        invoice.setSgstAmount(sgst);
        invoice.setTotalTax(order.getTaxAmount());
        invoice.setShippingAmount(order.getShippingAmount());
        invoice.setPackagingFee(order.getPackagingFee() != null ? order.getPackagingFee() : new BigDecimal("10.00"));
        invoice.setGrandTotal(order.getTotalAmount());
        invoice.setPaymentMethod(order.getPaymentMethod().name());
        invoice.setPaymentStatus(order.getPaymentStatus().name());

        return invoice;
    }

    private synchronized String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String searchPrefix = "ORD-" + datePrefix + "-";
        long count = orderRepository.countByOrderNumberStartingWith(searchPrefix) + 1;
        return String.format("ORD-%s-%04d", datePrefix, count);
    }
}
