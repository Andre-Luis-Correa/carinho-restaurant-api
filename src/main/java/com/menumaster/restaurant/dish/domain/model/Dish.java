package com.menumaster.restaurant.dish.domain.model;

import com.menumaster.restaurant.category.domain.model.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Dish {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private Double reaisPrice;

    @NotNull
    private Integer pointsPrice;

    @NotNull
    private Double reaisCostValue;

    @NotBlank
    private String urlImage;

    @NotNull
    private boolean isAvailable;

    @NotNull
    @ManyToOne
    private Category category;

    @NotNull
    @OneToMany
    private List<DishIngredient> dishIngredientList;
}
