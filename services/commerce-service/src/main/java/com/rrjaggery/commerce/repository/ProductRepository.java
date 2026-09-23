package com.rrjaggery.commerce.repository;

import com.rrjaggery.commerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);

    List<Product> findByActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(UUID categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchActiveProducts(@Param("query") String query);
}
