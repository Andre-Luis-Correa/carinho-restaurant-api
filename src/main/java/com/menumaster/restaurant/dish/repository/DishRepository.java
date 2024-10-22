package com.menumaster.restaurant.dish.repository;

import com.menumaster.restaurant.dish.domain.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByNameContainingIgnoreCase(String trimmedDish);

    Optional<Dish> findFirstByNameContainingIgnoreCase(String name);
}
