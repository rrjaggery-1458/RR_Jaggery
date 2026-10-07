package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.config.CustomerSecurityUtils;
import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.CreateReviewRequest;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.dto.ProductReviewDto;
import com.rrjaggery.commerce.service.CategoryService;
import com.rrjaggery.commerce.service.ProductService;
import com.rrjaggery.commerce.service.ReviewService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/commerce")
public class PublicCatalogueController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final CustomerSecurityUtils securityUtils;

    public PublicCatalogueController(CategoryService categoryService, ProductService productService,
                                   ReviewService reviewService, CustomerSecurityUtils securityUtils) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getActiveCategories()));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getActiveProducts(categoryId, query)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id)));
    }

    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getBySlug(slug)));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ProductReviewDto>>> getApprovedReviews(
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getApprovedReviews(productId)));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ProductReviewDto>> submitReview(
            @Valid @RequestBody CreateReviewRequest req,
            HttpServletRequest request) {
        UUID userId = null;
        String email = req.getCustomerEmail();
        String name = req.getCustomerName();

        try {
            CustomerSecurityUtils.AuthenticatedCustomer customer = securityUtils.getAuthenticatedCustomer(request);
            if (customer != null) {
                userId = customer.getUserId();
                if (email == null || email.isBlank()) email = customer.getEmail();
            }
        } catch (Exception ignored) {}

        ProductReviewDto created = reviewService.createReview(req, userId, email, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created, "Review submitted successfully."));
    }
}
