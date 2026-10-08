package com.rrjaggery.customerledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_payments", schema = "customer_schema")
public class CustomerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 100)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offline_order_id")
    private OfflineOrder offlineOrder;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate = Instant.now();

    @Column(name = "reference_notes")
    private String referenceNotes;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CustomerPayment() {}

    public CustomerPayment(String paymentNumber, Customer customer, OfflineOrder offlineOrder,
                           BigDecimal amount, String paymentMethod, String referenceNotes, String idempotencyKey) {
        this.paymentNumber = paymentNumber;
        this.customer = customer;
        this.offlineOrder = offlineOrder;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceNotes = referenceNotes;
        this.idempotencyKey = idempotencyKey;
        this.paymentDate = Instant.now();
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public OfflineOrder getOfflineOrder() { return offlineOrder; }
    public void setOfflineOrder(OfflineOrder offlineOrder) { this.offlineOrder = offlineOrder; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Instant getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Instant paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceNotes() { return referenceNotes; }
    public void setReferenceNotes(String referenceNotes) { this.referenceNotes = referenceNotes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
