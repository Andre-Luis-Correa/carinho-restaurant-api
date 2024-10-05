package com.menumaster.restaurant.ingredient.repository;

import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
