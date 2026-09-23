package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.CreateCategoryRequest;
import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : toSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Category category = new Category(request.getName(), slug, request.getDescription(), request.getDisplayOrder());
        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());

        Category updated = categoryRepository.save(category);
        return CategoryDto.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
