package com.rrjaggery.commerce.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_outbox", schema = "commerce_schema",
        uniqueConstraints = @UniqueConstraint(name = "uk_commerce_stock_outbox_command", columnNames = "command_key"))
public class StockOutboxCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_key", nullable = false, length = 255)
    private String commandKey;

    @Column(name = "depends_on_command_key", length = 255)
    private String dependsOnCommandKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    private StockOutboxOperation operation;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reference_id", nullable = false, length = 255)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockOutboxStatus status = StockOutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey; }
    public String getDependsOnCommandKey() { return dependsOnCommandKey; }
    public void setDependsOnCommandKey(String dependsOnCommandKey) { this.dependsOnCommandKey = dependsOnCommandKey; }
    public StockOutboxOperation getOperation() { return operation; }
    public void setOperation(StockOutboxOperation operation) { this.operation = operation; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public StockOutboxStatus getStatus() { return status; }
    public void setStatus(StockOutboxStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
