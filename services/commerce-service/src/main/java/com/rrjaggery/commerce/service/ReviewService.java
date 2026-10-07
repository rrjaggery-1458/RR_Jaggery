package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.CreateReviewRequest;
import com.rrjaggery.commerce.dto.ProductReviewDto;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductReview;
import com.rrjaggery.commerce.repository.ProductRepository;
import com.rrjaggery.commerce.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ProductReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductReviewDto> getApprovedReviews(UUID productId) {
        List<ProductReview> list;
        if (productId != null) {
            list = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "APPROVED");
        } else {
            list = reviewRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProductReviewDto createReview(CreateReviewRequest req, UUID userId, String authEmail, String authName) {
        String name = req.getCustomerName();
        if (name == null || name.isBlank()) {
            name = (authName != null && !authName.isBlank()) ? authName : "Verified Customer";
        }
        String email = req.getCustomerEmail();
        if (email == null || email.isBlank()) {
            email = authEmail;
        }

        String productName = req.getProductName();
        if ((productName == null || productName.isBlank()) && req.getProductId() != null) {
            productName = productRepository.findById(req.getProductId())
                    .map(Product::getName)
                    .orElse("RR Jaggery Product");
        } else if (productName == null || productName.isBlank()) {
            productName = "RR Organic Jaggery";
        }

        ProductReview review = new ProductReview(
                req.getProductId(),
                productName,
                userId,
                name,
                email,
                req.getRating(),
                req.getTitle(),
                req.getComment(),
                userId != null // Verified customer if authenticated
        );

        ProductReview saved = reviewRepository.save(review);
        return toDto(saved);
    }

    @Transactional
    public ProductReviewDto toggleReviewStatus(UUID id) {
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        if ("APPROVED".equals(review.getStatus())) {
            review.setStatus("HIDDEN");
        } else {
            review.setStatus("APPROVED");
        }
        return toDto(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(UUID id) {
        if (!reviewRepository.existsById(id)) {
            throw new IllegalArgumentException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    private ProductReviewDto toDto(ProductReview entity) {
        ProductReviewDto dto = new ProductReviewDto();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setUserId(entity.getUserId());
        dto.setCustomerName(entity.getCustomerName());
        dto.setRating(entity.getRating());
        dto.setTitle(entity.getTitle());
        dto.setComment(entity.getComment());
        dto.setVerifiedPurchase(entity.isVerifiedPurchase());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
