package com.menumaster.restaurant.dish.service;

import com.menumaster.restaurant.dish.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
}
