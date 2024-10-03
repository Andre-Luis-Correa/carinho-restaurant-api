package com.menumaster.restaurant.ingredient.domain.model;

import com.menumaster.restaurant.measurementunit.domain.model.MeasurementUnit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Double totalQuantityAvailable;

    @NotNull
    @ManyToOne
    private MeasurementUnit measurementUnit;

}
