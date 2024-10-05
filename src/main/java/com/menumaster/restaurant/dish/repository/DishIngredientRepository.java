package com.menumaster.restaurant.dish.repository;

import com.menumaster.restaurant.dish.domain.model.Dish;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishIngredientRepository extends JpaRepository<DishIngredient, Long> {

    boolean
    existsDishIngredientByDish(Dish dish);

    List<DishIngredient> findAllByDish(Dish dish);
}
