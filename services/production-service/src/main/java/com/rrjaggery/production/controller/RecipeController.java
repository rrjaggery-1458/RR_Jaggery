package com.rrjaggery.production.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.production.dto.CreateRecipeRequest;
import com.rrjaggery.production.dto.RecipeDto;
import com.rrjaggery.production.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/production/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<RecipeDto>> createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        RecipeDto created = recipeService.createRecipe(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Recipe created successfully."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<RecipeDto>>> getAllRecipes() {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.getAllRecipes()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<RecipeDto>> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.getRecipeById(id)));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<RecipeDto>> getRecipeByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.getRecipeByCode(code)));
    }

    @GetMapping("/product/{sku}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','PRODUCTION_MANAGER')")
    public ResponseEntity<ApiResponse<List<RecipeDto>>> getRecipesByOutputSku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.ok(recipeService.getRecipesByOutputSku(sku)));
    }
}
