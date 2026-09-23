package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.service.CategoryService;
import com.rrjaggery.commerce.service.ProductService;
import com.rrjaggery.common.dto.ApiResponse;
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

    public PublicCatalogueController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
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
}
