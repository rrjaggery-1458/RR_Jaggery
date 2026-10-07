package com.rrjaggery.commerce.repository;

import com.rrjaggery.commerce.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    List<ProductReview> findByStatusOrderByCreatedAtDesc(String status);
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, String status);
    List<ProductReview> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
