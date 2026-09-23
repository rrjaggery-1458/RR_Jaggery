package com.rrjaggery.commerce.repository;

import com.rrjaggery.commerce.entity.Order;
import com.rrjaggery.commerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByOrderNumberStartingWith(String prefix);
}
