package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.OfflineOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfflineOrderRepository extends JpaRepository<OfflineOrder, UUID> {

    List<OfflineOrder> findByCustomerIdOrderByOrderDateDesc(UUID customerId);

    List<OfflineOrder> findAllByOrderByOrderDateDesc();

    Optional<OfflineOrder> findByOrderNumber(String orderNumber);

    Optional<OfflineOrder> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT o FROM OfflineOrder o WHERE o.customer.id = :customerId AND o.outstandingAmount > 0 AND o.dueDate < :now")
    List<OfflineOrder> findOverdueOrdersForCustomer(@Param("customerId") UUID customerId, @Param("now") Instant now);

    @Query("SELECT o FROM OfflineOrder o WHERE o.outstandingAmount > 0 AND o.dueDate < :now")
    List<OfflineOrder> findAllOverdueOrders(@Param("now") Instant now);
}
