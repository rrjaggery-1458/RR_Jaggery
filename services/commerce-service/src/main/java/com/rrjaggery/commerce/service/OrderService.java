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

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% total GST on jaggery
    private static final BigDecimal HALF_GST_RATE = new BigDecimal("0.025"); // 2.5% CGST & 2.5% SGST
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("50.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryStockClient inventoryStockClient;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            InventoryStockClient inventoryStockClient
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
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
        order.setStreetAddress(addr.getStreetAddress());
        order.setCity(addr.getCity());
        order.setState(addr.getState());
        order.setPostalCode(addr.getPostalCode());
        order.setNotes(request.getNotes());

        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.TEST_PAYMENT;
        order.setPaymentMethod(paymentMethod);

        if (paymentMethod == PaymentMethod.TEST_PAYMENT) {
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

        BigDecimal shipping = BigDecimal.ZERO;
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0) {
            shipping = STANDARD_SHIPPING_FEE;
        }

        BigDecimal grandTotal = subtotal.add(totalTax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(totalTax);
        order.setShippingAmount(shipping);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(grandTotal);

        Order saved = orderRepository.save(order);

        if (saved.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : saved.getItems()) {
                inventoryStockClient.deductSaleStock(
                        item.getSku(),
                        BigDecimal.valueOf(item.getQuantity()),
                        saved.getOrderNumber() + ":" + item.getId(),
                        saved.getOrderNumber()
                );
            }
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

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus() + ". Only PENDING or CONFIRMED orders can be cancelled.");
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

    public OrderDto updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus previousStatus = order.getStatus();
        if (newStatus == OrderStatus.CONFIRMED && previousStatus != OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                inventoryStockClient.deductSaleStock(
                        item.getSku(),
                        BigDecimal.valueOf(item.getQuantity()),
                        order.getOrderNumber() + ":" + item.getId(),
                        order.getOrderNumber()
                );
            }
        }
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        Order saved = orderRepository.save(order);
        return OrderDto.fromEntity(saved);
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
                order.getStreetAddress(),
                order.getCity(),
                order.getState(),
                order.getPostalCode()
        ));

        invoice.setItems(order.getItems().stream().map(OrderItemDto::fromEntity).collect(Collectors.toList()));
        invoice.setSubtotal(order.getSubtotalAmount());

        BigDecimal cgst = order.getSubtotalAmount().multiply(HALF_GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = order.getSubtotalAmount().multiply(HALF_GST_RATE).setScale(2, RoundingMode.HALF_UP);

        invoice.setCgstAmount(cgst);
        invoice.setSgstAmount(sgst);
        invoice.setTotalTax(order.getTaxAmount());
        invoice.setShippingAmount(order.getShippingAmount());
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
