package com.rrjaggery.production.service;

import com.rrjaggery.production.dto.CreateRecipeRequest;
import com.rrjaggery.production.dto.RecipeDto;
import com.rrjaggery.production.dto.RecipeItemDto;
import com.rrjaggery.production.dto.RecipeItemRequest;
import com.rrjaggery.production.entity.Recipe;
import com.rrjaggery.production.entity.RecipeItem;
import com.rrjaggery.production.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional
    public RecipeDto createRecipe(CreateRecipeRequest request) {
        String recipeCode = normalizeCode(request.getRecipeCode());
        if (recipeRepository.existsByRecipeCodeIgnoreCase(recipeCode)) {
            throw new IllegalArgumentException("Recipe with code '" + recipeCode + "' already exists.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one bill of materials item.");
        }

        Recipe recipe = new Recipe();
        recipe.setRecipeCode(recipeCode);
        recipe.setRecipeName(request.getRecipeName().trim());
        recipe.setOutputProductSku(normalizeCode(request.getOutputProductSku()));
        recipe.setOutputProductName(request.getOutputProductName().trim());
        recipe.setStandardBatchSize(request.getStandardBatchSize());
        recipe.setUnitOfMeasure(request.getUnitOfMeasure() != null ? request.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
        recipe.setStandardYieldPercentage(request.getStandardYieldPercentage() != null ? request.getStandardYieldPercentage() : new BigDecimal("100.00"));
        recipe.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 60);
        recipe.setDescription(request.getDescription());
        recipe.setIsActive(true);
        recipe.setCreatedAt(Instant.now());
        recipe.setUpdatedAt(Instant.now());

        for (RecipeItemRequest itemReq : request.getItems()) {
            RecipeItem item = new RecipeItem();
            item.setRecipe(recipe);
            item.setRawMaterialSku(normalizeCode(itemReq.getRawMaterialSku()));
            item.setRawMaterialName(itemReq.getRawMaterialName().trim());
            item.setRequiredQuantity(itemReq.getRequiredQuantity());
            item.setUnitOfMeasure(itemReq.getUnitOfMeasure() != null ? itemReq.getUnitOfMeasure().trim().toUpperCase(Locale.ROOT) : "KG");
            item.setNotes(itemReq.getNotes());
            recipe.getItems().add(item);
        }

        Recipe saved = recipeRepository.save(recipe);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RecipeDto> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecipeDto getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found for ID: " + id));
        return mapToDto(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeDto getRecipeByCode(String code) {
        String normalized = normalizeCode(code);
        Recipe recipe = recipeRepository.findByRecipeCodeIgnoreCase(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found for code: " + code));
        return mapToDto(recipe);
    }

    @Transactional(readOnly = true)
    public List<RecipeDto> getRecipesByOutputSku(String sku) {
        return recipeRepository.findByOutputProductSku(normalizeCode(sku)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RecipeDto mapToDto(Recipe recipe) {
        RecipeDto dto = new RecipeDto();
        dto.setId(recipe.getId());
        dto.setRecipeCode(recipe.getRecipeCode());
        dto.setRecipeName(recipe.getRecipeName());
        dto.setOutputProductSku(recipe.getOutputProductSku());
        dto.setOutputProductName(recipe.getOutputProductName());
        dto.setStandardBatchSize(recipe.getStandardBatchSize());
        dto.setUnitOfMeasure(recipe.getUnitOfMeasure());
        dto.setStandardYieldPercentage(recipe.getStandardYieldPercentage());
        dto.setEstimatedDurationMinutes(recipe.getEstimatedDurationMinutes());
        dto.setIsActive(recipe.getIsActive());
        dto.setDescription(recipe.getDescription());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setUpdatedAt(recipe.getUpdatedAt());

        if (recipe.getItems() != null) {
            dto.setItems(recipe.getItems().stream()
                    .map(item -> new RecipeItemDto(
                            item.getId(),
                            item.getRawMaterialSku(),
                            item.getRawMaterialName(),
                            item.getRequiredQuantity(),
                            item.getUnitOfMeasure(),
                            item.getNotes()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code or SKU cannot be blank.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
