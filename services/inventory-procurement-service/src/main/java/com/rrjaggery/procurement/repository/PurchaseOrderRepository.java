package com.rrjaggery.procurement.repository;

import com.rrjaggery.procurement.entity.PurchaseOrder;
import com.rrjaggery.procurement.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
    List<PurchaseOrder> findByStatusOrderByOrderDateDesc(PurchaseOrderStatus status);
    List<PurchaseOrder> findAllByOrderByOrderDateDesc();
}
