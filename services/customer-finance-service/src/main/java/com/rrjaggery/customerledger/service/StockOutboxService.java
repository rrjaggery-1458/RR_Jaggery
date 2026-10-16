package com.rrjaggery.customerledger.service;

import com.rrjaggery.customerledger.entity.OfflineOrder;
import com.rrjaggery.customerledger.entity.OfflineOrderItem;
import com.rrjaggery.customerledger.entity.StockOutboxCommand;
import com.rrjaggery.customerledger.entity.StockOutboxOperation;
import com.rrjaggery.customerledger.entity.StockOutboxStatus;
import com.rrjaggery.customerledger.repository.StockOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class StockOutboxService {

    private static final Logger log = LoggerFactory.getLogger(StockOutboxService.class);
    private final StockOutboxRepository repository;
    private final OfflineInventoryStockClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    public StockOutboxService(StockOutboxRepository repository, OfflineInventoryStockClient inventoryClient,
                              ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
    }

    public void enqueueSale(OfflineOrder order) {
        for (OfflineOrderItem item : order.getItems()) {
            BigDecimal weight = item.getUnitWeightKg();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Order item must have a positive unitWeightKg before stock can be sold.");
            }
            String referenceId = order.getOrderNumber() + ":" + item.getId();
            enqueue("SALE:" + referenceId, null, StockOutboxOperation.DEDUCT, item.getSku(),
                    BigDecimal.valueOf(item.getQuantity()).multiply(weight).setScale(3, RoundingMode.HALF_UP),
                    referenceId);
        }
        eventPublisher.publishEvent(new StockOutboxSignal());
    }

    public void enqueueRestoration(OfflineOrder order) {
        for (OfflineOrderItem item : order.getItems()) {
            String referenceId = order.getOrderNumber() + ":" + item.getId();
            String saleKey = "SALE:" + referenceId;
            if (repository.findByCommandKey(saleKey).isEmpty()) {
                continue;
            }
            BigDecimal weight = item.getUnitWeightKg();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Order item must have a positive unitWeightKg before stock can be restored.");
            }
            enqueue("RESTORE:" + referenceId, saleKey, StockOutboxOperation.RESTORE, item.getSku(),
                    BigDecimal.valueOf(item.getQuantity()).multiply(weight).setScale(3, RoundingMode.HALF_UP),
                    referenceId);
        }
        eventPublisher.publishEvent(new StockOutboxSignal());
    }

    private void enqueue(String key, String dependsOn, StockOutboxOperation operation, String sku,
                         BigDecimal quantity, String referenceId) {
        if (repository.findByCommandKey(key).isPresent()) {
            return;
        }
        StockOutboxCommand command = new StockOutboxCommand();
        command.setCommandKey(key);
        command.setDependsOnCommandKey(dependsOn);
        command.setOperation(operation);
        command.setSku(sku);
        command.setQuantity(quantity);
        command.setReferenceId(referenceId);
        command.setStatus(StockOutboxStatus.PENDING);
        command.setNextAttemptAt(Instant.now());
        repository.save(command);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(StockOutboxSignal ignored) {
        dispatchPending();
    }

    @Scheduled(fixedDelayString = "${app.inventory.outbox.poll-interval-ms:3000}")
    public void dispatchPending() {
        List<StockOutboxCommand> commands = repository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(StockOutboxStatus.PENDING, Instant.now());
        for (StockOutboxCommand command : commands) {
            dispatch(command);
        }
    }

    private synchronized void dispatch(StockOutboxCommand command) {
        if (command.getDependsOnCommandKey() != null) {
            StockOutboxCommand dependency = repository.findByCommandKey(command.getDependsOnCommandKey()).orElse(null);
            if (dependency == null) {
                fail(command, "Required stock deduction command is missing.");
                return;
            }
            if (dependency.getStatus() == StockOutboxStatus.PENDING) {
                Instant retryAt = Instant.now().plusSeconds(3);
                if (dependency.getNextAttemptAt() != null && dependency.getNextAttemptAt().isAfter(retryAt)) {
                    retryAt = dependency.getNextAttemptAt();
                }
                command.setNextAttemptAt(retryAt);
                repository.save(command);
                return;
            }
            if (dependency.getStatus() == StockOutboxStatus.FAILED) {
                fail(command, "Stock was not deducted; cancellation restoration was not applied.");
                return;
            }
        }

        try {
            if (command.getOperation() == StockOutboxOperation.DEDUCT) {
                inventoryClient.deductOfflineOrderStock(command.getSku(), command.getQuantity(), command.getReferenceId());
            } else {
                inventoryClient.restoreOfflineOrderStock(command.getSku(), command.getQuantity(), command.getReferenceId());
            }
            command.setStatus(StockOutboxStatus.SUCCEEDED);
            command.setLastError(null);
            command.setProcessedAt(Instant.now());
            repository.save(command);
        } catch (RuntimeException ex) {
            command.setAttempts(command.getAttempts() + 1);
            command.setLastError(truncate(ex.getMessage()));
            if (isPermanentClientError(ex)) {
                command.setStatus(StockOutboxStatus.FAILED);
                command.setProcessedAt(Instant.now());
                log.error("Permanent offline stock command failure {}: {}", command.getCommandKey(), ex.getMessage());
            } else {
                long delaySeconds = Math.min(300, 1L << Math.min(command.getAttempts(), 8));
                command.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                log.warn("Offline stock command {} will retry after failure: {}", command.getCommandKey(), ex.getMessage());
            }
            repository.save(command);
        }
    }

    private void fail(StockOutboxCommand command, String reason) {
        command.setStatus(StockOutboxStatus.FAILED);
        command.setLastError(reason);
        command.setProcessedAt(Instant.now());
        repository.save(command);
        log.error("Offline stock command {} was not applied: {}", command.getCommandKey(), reason);
    }

    private boolean isPermanentClientError(RuntimeException ex) {
        if (!(ex instanceof HttpStatusCodeException responseException)) {
            return false;
        }
        HttpStatusCode status = responseException.getStatusCode();
        return status.is4xxClientError() && status.value() != 408 && status.value() != 429;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Inventory service request failed.";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
