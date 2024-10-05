package com.menumaster.restaurant.dish.domain.model;

import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DishIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    private Dish dish;

    @NotNull
    @ManyToOne
    private Ingredient ingredient;

    @NotNull
    private Double quantity;
}
