package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    Optional<Recipe> findByRecipeCode(String recipeCode);
    Optional<Recipe> findByRecipeCodeIgnoreCase(String recipeCode);
    List<Recipe> findByOutputProductSku(String outputProductSku);
    List<Recipe> findByActiveTrue();
    boolean existsByRecipeCodeIgnoreCase(String recipeCode);
}
