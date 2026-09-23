package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.CreateCategoryRequest;
import com.rrjaggery.commerce.dto.CreateProductRequest;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.service.CategoryService;
import com.rrjaggery.commerce.service.ProductService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/commerce/admin")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminCatalogueController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public AdminCatalogueController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto dto = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto, "Category created successfully"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto dto = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Category updated successfully"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deactivated successfully"));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProductsAdmin()));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto dto = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto, "Product created successfully"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto dto = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Product updated successfully"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deactivated successfully"));
    }
}
