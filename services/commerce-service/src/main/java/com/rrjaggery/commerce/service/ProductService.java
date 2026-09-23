package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.CreateProductRequest;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.repository.CategoryRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getActiveProducts(UUID categoryId, String query) {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<Product> products;
        if (query != null && !query.isBlank()) {
            products = productRepository.searchActiveProducts(query.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        } else {
            products = productRepository.findByActiveTrue();
        }

        return products.stream()
                .map(p -> ProductDto.fromEntity(p, categoryNames.getOrDefault(p.getCategoryId(), "General")))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProductsAdmin() {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        return productRepository.findAll().stream()
                .map(p -> ProductDto.fromEntity(p, categoryNames.getOrDefault(p.getCategoryId(), "General")))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        String catName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse("General");
        return ProductDto.fromEntity(product, catName);
    }

    @Transactional(readOnly = true)
    public ProductDto getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with slug: " + slug));
        String catName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse("General");
        return ProductDto.fromEntity(product, catName);
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku().toUpperCase().trim())) {
            throw new IllegalArgumentException("Product SKU already exists: " + request.getSku());
        }

        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : toSlug(request.getName());

        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Invalid category ID: " + request.getCategoryId());
        }

        Product product = new Product();
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setSku(request.getSku().toUpperCase().trim());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setGrade(request.getGrade());
        product.setPackageType(request.getPackageType());
        product.setUnitWeightKg(request.getUnitWeightKg());
        product.setRetailPrice(request.getRetailPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setWholesaleMoq(request.getWholesaleMoq());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        String catName = categoryRepository.findById(saved.getCategoryId()).map(Category::getName).orElse("General");
        return ProductDto.fromEntity(saved, catName);
    }

    @Transactional
    public ProductDto updateProduct(UUID id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setGrade(request.getGrade());
        product.setPackageType(request.getPackageType());
        product.setUnitWeightKg(request.getUnitWeightKg());
        product.setRetailPrice(request.getRetailPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setWholesaleMoq(request.getWholesaleMoq());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());

        Product updated = productRepository.save(product);
        String catName = categoryRepository.findById(updated.getCategoryId()).map(Category::getName).orElse("General");
        return ProductDto.fromEntity(updated, catName);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
