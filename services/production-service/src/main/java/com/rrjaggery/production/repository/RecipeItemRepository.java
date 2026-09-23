package com.rrjaggery.production.repository;

import com.rrjaggery.production.entity.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {
    List<RecipeItem> findByRecipeId(UUID recipeId);
}
