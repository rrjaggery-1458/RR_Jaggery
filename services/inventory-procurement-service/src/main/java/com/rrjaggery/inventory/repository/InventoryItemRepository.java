package com.rrjaggery.inventory.repository;

import com.rrjaggery.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findBySku(String sku);
    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.sku = :sku")
    Optional<InventoryItem> findBySkuForUpdate(@Param("sku") String sku);
}
