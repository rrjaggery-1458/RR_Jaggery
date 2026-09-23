package com.rrjaggery.procurement.repository;

import com.rrjaggery.procurement.entity.GoodsReceipt;
import com.rrjaggery.procurement.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {
    List<GoodsReceipt> findByPurchaseOrderOrderByReceiptTimestampDesc(PurchaseOrder purchaseOrder);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
