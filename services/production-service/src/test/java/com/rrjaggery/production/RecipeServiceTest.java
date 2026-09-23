package com.rrjaggery.production;

import com.rrjaggery.production.dto.CreateRecipeRequest;
import com.rrjaggery.production.dto.RecipeDto;
import com.rrjaggery.production.dto.RecipeItemRequest;
import com.rrjaggery.production.entity.Recipe;
import com.rrjaggery.production.repository.RecipeRepository;
import com.rrjaggery.production.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository);
    }

    @Test
    void createRecipe_Success() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setRecipeCode("RCP-ORGANIC-500G");
        request.setRecipeName("Organic Jaggery 500g Blocks Recipe");
        request.setOutputProductSku("JAG-ORG-500G");
        request.setOutputProductName("Organic Jaggery 500g");
        request.setStandardBatchSize(new BigDecimal("100.00"));
        request.setUnitOfMeasure("KG");
        request.setStandardYieldPercentage(new BigDecimal("95.00"));
        request.setEstimatedDurationMinutes(120);
        request.setItems(List.of(
                new RecipeItemRequest("RAW-CANE-01", "Raw Sugarcane Juice", new BigDecimal("120.00"), "KG")
        ));

        when(recipeRepository.existsByRecipeCodeIgnoreCase("RCP-ORGANIC-500G")).thenReturn(false);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RecipeDto result = recipeService.createRecipe(request);

        assertNotNull(result);
        assertEquals("RCP-ORGANIC-500G", result.getRecipeCode());
        assertEquals("JAG-ORG-500G", result.getOutputProductSku());
        assertEquals(1, result.getItems().size());
        assertEquals("RAW-CANE-01", result.getItems().get(0).getRawMaterialSku());
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void createRecipe_DuplicateCode_ThrowsException() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setRecipeCode("RCP-DUPLICATE");
        request.setRecipeName("Duplicate Recipe");
        request.setOutputProductSku("JAG-DUP");
        request.setOutputProductName("Duplicate");
        request.setStandardBatchSize(new BigDecimal("50.00"));
        request.setItems(List.of(new RecipeItemRequest("RAW-1", "Raw 1", new BigDecimal("50.00"), "KG")));

        when(recipeRepository.existsByRecipeCodeIgnoreCase("RCP-DUPLICATE")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> recipeService.createRecipe(request));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void getRecipeByCode_Found() {
        Recipe recipe = new Recipe();
        recipe.setId(UUID.randomUUID());
        recipe.setRecipeCode("RCP-001");
        recipe.setRecipeName("Recipe 1");
        recipe.setOutputProductSku("SKU-1");
        recipe.setOutputProductName("Prod 1");
        recipe.setStandardBatchSize(new BigDecimal("100.00"));
        recipe.setUnitOfMeasure("KG");

        when(recipeRepository.findByRecipeCodeIgnoreCase("RCP-001")).thenReturn(Optional.of(recipe));

        RecipeDto dto = recipeService.getRecipeByCode("RCP-001");
        assertNotNull(dto);
        assertEquals("RCP-001", dto.getRecipeCode());
    }
}
