package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.OfflineOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfflineOrderItemRepository extends JpaRepository<OfflineOrderItem, UUID> {
    List<OfflineOrderItem> findByOfflineOrderId(UUID offlineOrderId);
}
