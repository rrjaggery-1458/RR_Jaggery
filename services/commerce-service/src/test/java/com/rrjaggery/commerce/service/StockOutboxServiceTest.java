package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.entity.StockOutboxCommand;
import com.rrjaggery.commerce.entity.StockOutboxOperation;
import com.rrjaggery.commerce.entity.StockOutboxStatus;
import com.rrjaggery.commerce.repository.StockOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockOutboxServiceTest {

    @Mock
    private StockOutboxRepository repository;
    @Mock
    private InventoryStockClient inventoryStockClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private StockOutboxService service;

    private StockOutboxCommand command;

    @BeforeEach
    void setUp() {
        command = new StockOutboxCommand();
        command.setId(1L);
        command.setCommandKey("SALE:ORDER-1:LINE-1");
        command.setOperation(StockOutboxOperation.DEDUCT);
        command.setSku("JAG-1KG");
        command.setQuantity(new BigDecimal("1.000"));
        command.setReferenceId("ORDER-1:LINE-1");
        command.setStatus(StockOutboxStatus.PENDING);
        command.setNextAttemptAt(Instant.now().minusSeconds(1));
        when(repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                eq(StockOutboxStatus.PENDING), any(Instant.class)))
                .thenAnswer(invocation -> command.getStatus() == StockOutboxStatus.PENDING
                        && !command.getNextAttemptAt().isAfter(invocation.getArgument(1))
                        ? List.of(command) : List.of());
    }

    @Test
    void transientFailureSetsBackoffAndLaterRetrySucceeds() {
        doThrow(new ResourceAccessException("inventory unavailable"))
                .doNothing()
                .when(inventoryStockClient)
                .deductSaleStock(command.getSku(), command.getQuantity(), command.getReferenceId(), "COMMERCE");

        Instant beforeDispatch = Instant.now();
        service.dispatchPending();

        assertEquals(StockOutboxStatus.PENDING, command.getStatus());
        assertEquals(1, command.getAttempts());
        assertNotNull(command.getLastError());
        assertTrue(command.getNextAttemptAt().isAfter(beforeDispatch.plusSeconds(1)));

        command.setNextAttemptAt(Instant.now().minusSeconds(1));
        service.dispatchPending();

        assertEquals(StockOutboxStatus.SUCCEEDED, command.getStatus());
        assertEquals(1, command.getAttempts());
        assertNotNull(command.getProcessedAt());
        verify(inventoryStockClient, times(2))
                .deductSaleStock(command.getSku(), command.getQuantity(), command.getReferenceId(), "COMMERCE");
    }

    @Test
    void permanentClientFailureIsNotRetried() {
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid SKU"))
                .when(inventoryStockClient)
                .deductSaleStock(command.getSku(), command.getQuantity(), command.getReferenceId(), "COMMERCE");

        service.dispatchPending();

        assertEquals(StockOutboxStatus.FAILED, command.getStatus());
        assertEquals(1, command.getAttempts());
        assertNotNull(command.getProcessedAt());

        service.dispatchPending();
        verify(inventoryStockClient, times(1))
                .deductSaleStock(command.getSku(), command.getQuantity(), command.getReferenceId(), "COMMERCE");
    }
}
