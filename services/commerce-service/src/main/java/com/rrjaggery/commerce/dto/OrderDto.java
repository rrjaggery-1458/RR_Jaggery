package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.Order;
import com.rrjaggery.commerce.entity.OrderStatus;
import com.rrjaggery.commerce.entity.PaymentMethod;
import com.rrjaggery.commerce.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderDto {

    private UUID id;
    private String orderNumber;
    private UUID userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerType;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private AddressDto shippingAddress;
    private String notes;
    private List<OrderItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;

    public OrderDto() {}

    public static OrderDto fromEntity(Order order) {
        OrderDto dto = new OrderDto();
        dto.id = order.getId();
        dto.orderNumber = order.getOrderNumber();
        dto.userId = order.getUserId();
        dto.customerName = order.getCustomerName();
        dto.customerEmail = order.getCustomerEmail();
        dto.customerPhone = order.getCustomerPhone();
        dto.customerType = order.getCustomerType();
        dto.subtotalAmount = order.getSubtotalAmount();
        dto.taxAmount = order.getTaxAmount();
        dto.shippingAmount = order.getShippingAmount();
        dto.discountAmount = order.getDiscountAmount();
        dto.totalAmount = order.getTotalAmount();
        dto.status = order.getStatus();
        dto.paymentStatus = order.getPaymentStatus();
        dto.paymentMethod = order.getPaymentMethod();
        dto.shippingAddress = new AddressDto(
            order.getRecipientName(),
            order.getPhone(),
            order.getStreetAddress(),
            order.getCity(),
            order.getState(),
            order.getPostalCode()
        );
        dto.notes = order.getNotes();
        dto.items = order.getItems().stream().map(OrderItemDto::fromEntity).collect(Collectors.toList());
        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        return dto;
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public UUID getUserId() { return userId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerType() { return customerType; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public AddressDto getShippingAddress() { return shippingAddress; }
    public String getNotes() { return notes; }
    public List<OrderItemDto> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
