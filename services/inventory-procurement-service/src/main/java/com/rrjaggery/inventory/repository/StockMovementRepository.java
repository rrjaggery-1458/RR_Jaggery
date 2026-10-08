package com.rrjaggery.inventory.repository;

import com.rrjaggery.inventory.entity.InventoryItem;
import com.rrjaggery.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByItemOrderByMovementTimeDesc(InventoryItem item);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
