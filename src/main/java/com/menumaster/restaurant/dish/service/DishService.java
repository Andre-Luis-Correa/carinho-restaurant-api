package com.menumaster.restaurant.dish.service;

import com.menumaster.restaurant.category.domain.model.Category;
import com.menumaster.restaurant.dish.domain.dto.DishDTO;
import com.menumaster.restaurant.dish.domain.dto.DishFormDTO;
import com.menumaster.restaurant.dish.domain.dto.DishIngredientDTO;
import com.menumaster.restaurant.dish.domain.dto.DishIngredientFormDTO;
import com.menumaster.restaurant.dish.domain.model.Dish;
import com.menumaster.restaurant.dish.domain.model.DishIngredient;
import com.menumaster.restaurant.dish.mapper.DishFormDTOToDishMapper;
import com.menumaster.restaurant.dish.mapper.DishIngredientFormDTOToDishIngredientMapper;
import com.menumaster.restaurant.dish.mapper.DishIngredientToDishIngredientDTOMapper;
import com.menumaster.restaurant.dish.mapper.DishToDishDTOMapper;
import com.menumaster.restaurant.dish.repository.DishIngredientRepository;
import com.menumaster.restaurant.dish.repository.DishRepository;
import com.menumaster.restaurant.exception.type.EntityNotFoundException;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final DishFormDTOToDishMapper dishFormDTOToDishMapper;
    private final DishToDishDTOMapper dishToDishDTOMapper;
    private final DishIngredientFormDTOToDishIngredientMapper dishIngredientFormDTOToDishIngredientMapper;
    private final DishIngredientToDishIngredientDTOMapper dishIngredientToDishIngredientDTOMapper;

    public DishDTO create(Category category, List<DishIngredient> dishIngredientList, DishFormDTO dishFormDTO) {
        Dish dishToBeSaved = convertDishFormDTOToDish(dishFormDTO);
        dishToBeSaved.setCategory(category);
        Dish dishSaved = dishRepository.save(dishToBeSaved);

        saveDishIngredientList(dishIngredientList, dishSaved);

        return convertDishToDishDTO(dishSaved);
    }

    public DishDTO convertDishToDishDTO(Dish dish) {
        List<DishIngredient> dishIngredientList = dishIngredientRepository.findAllByDish(dish);
        List<DishIngredientDTO> dishIngredientDTOList = convertDishIngredientToDishIngredientDTO(dishIngredientList);
        return dishToDishDTOMapper.convert(dish, dishIngredientDTOList);
    }

    private List<DishIngredientDTO> convertDishIngredientToDishIngredientDTO(List<DishIngredient> dishIngredientList) {
        return dishIngredientList.stream().map(dishIngredientToDishIngredientDTOMapper::convert).toList();
    }

    private List<DishIngredient> saveDishIngredientList(List<DishIngredient> dishIngredientList, Dish dish) {
        for(DishIngredient dishIngredient : dishIngredientList) {
            dishIngredient.setDish(dish);
        }
        return dishIngredientRepository.saveAll(dishIngredientList);
    }

    public Dish convertDishFormDTOToDish(DishFormDTO dishFormDTO) {
        return dishFormDTOToDishMapper.convert(dishFormDTO);
    }

    public DishIngredient convertToDishIngredient(Dish dish, Ingredient ingredient, DishIngredientFormDTO dishIngredientFormDTO) {
        DishIngredient dishIngredient = dishIngredientFormDTOToDishIngredientMapper.convert(dishIngredientFormDTO);
        dishIngredient.setDish(dish);
        dishIngredient.setIngredient(ingredient);
        return dishIngredient;
    }

    public Dish getOrThrowException(Long id) {
        return dishRepository.findById(id).orElseThrow( () -> new EntityNotFoundException("Dish", id.toString()));
    }

    public DishDTO update(Dish dish, Category category, List<DishIngredient> dishIngredientList, DishFormDTO dishFormDTO) {
        if(dishFormDTO.name() != null && !dishFormDTO.name().isBlank()) {
            dish.setName(dishFormDTO.name());
        }
        if(dishFormDTO.description() != null && !dishFormDTO.description().isBlank()) {
            dish.setDescription(dishFormDTO.description());
        }
        if(dishFormDTO.reaisPrice() != null) {
            dish.setReaisPrice(dishFormDTO.reaisPrice());
        }
        if(dishFormDTO.pointsPrice() != null) {
            dish.setPointsPrice(dishFormDTO.pointsPrice());
        }
        if(dishFormDTO.reaisCostValue() != null) {
            dish.setReaisCostValue(dishFormDTO.reaisCostValue());
        }
        if(dishFormDTO.urlImage() != null && !dishFormDTO.urlImage().isBlank()) {
            dish.setUrlImage(dishFormDTO.urlImage());
        }
        if(dishFormDTO.isAvailable() != null) {
            dish.setIsAvailable(dishFormDTO.isAvailable());
        }
        if(category != null) {
            dish.setCategory(category);
        }

        updateDishIngredientList(dish, dishIngredientList);

        Dish dishUpdated = dishRepository.save(dish);
        return convertDishToDishDTO(dishUpdated);
    }

    private void updateDishIngredientList(Dish dish, List<DishIngredient> newDishIngredientList) {
        List<DishIngredient> existingDishIngredients = dishIngredientRepository.findAllByDish(dish);

        Map<Long, DishIngredient> newIngredientsMap = newDishIngredientList.stream()
                .collect(Collectors.toMap(di -> di.getIngredient().getId(), di -> di));

        Iterator<DishIngredient> iterator = existingDishIngredients.iterator();

        while (iterator.hasNext()) {
            DishIngredient existingIngredient = iterator.next();
            Long ingredientId = existingIngredient.getIngredient().getId();

            if (newIngredientsMap.containsKey(ingredientId)) {
                DishIngredient newIngredient = newIngredientsMap.get(ingredientId);
                existingIngredient.setQuantity(newIngredient.getQuantity());
                dishIngredientRepository.save(existingIngredient);
                newIngredientsMap.remove(ingredientId);
            }
        }

        for (DishIngredient newIngredient : newIngredientsMap.values()) {
            newIngredient.setDish(dish);
            newIngredient.setIngredient(newIngredient.getIngredient());
            newIngredient.setQuantity(newIngredient.getQuantity());
            existingDishIngredients.add(dishIngredientRepository.save(newIngredient));
        }
    }

    public List<DishDTO> list() {
        List<Dish> dishList = dishRepository.findAll();
        return dishList.stream().map(this::convertDishToDishDTO).toList();
    }

    public void delete(Dish dish) {
        List<DishIngredient> dishIngredientList = dishIngredientRepository.findAllByDish(dish);
        dishIngredientRepository.deleteAll(dishIngredientList);
        dishRepository.delete(dish);
    }

    public Page<DishDTO> listByPageable(Pageable pageable) {
        Page<Dish> dishDTOPage = dishRepository.findAll(pageable);
        return dishDTOPage.map(this::convertDishToDishDTO);
    }

    public DishDTO removeDishIngredient(Dish dish, List<DishIngredient> dishIngredientList) {
        List<DishIngredient> dishIngredients = dishIngredientRepository.findAllByDish(dish);

        for(DishIngredient dishIngredient : dishIngredientList) {
            if(dishIngredients.contains(dishIngredient)) {
                dishIngredientRepository.delete(dishIngredient);
            }
        }

        return convertDishToDishDTO(dish);
    }

    public DishIngredient getOrThrowExceptionByDishIngredientId(Long id) {
        return dishIngredientRepository.findById(id).orElseThrow( () -> new EntityNotFoundException("DishIngredient", id.toString()));

    }
}
