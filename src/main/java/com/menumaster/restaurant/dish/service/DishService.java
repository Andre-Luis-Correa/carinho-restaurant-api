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
import com.menumaster.restaurant.exception.type.DuplicateKeyException;
import com.menumaster.restaurant.exception.type.EntityNotFoundException;
import com.menumaster.restaurant.exception.type.UploadImageException;
import com.menumaster.restaurant.ingredient.domain.model.Ingredient;
import com.menumaster.restaurant.utils.ImageUtil;
import com.menumaster.restaurant.utils.UploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final DishFormDTOToDishMapper dishFormDTOToDishMapper;
    private final DishToDishDTOMapper dishToDishDTOMapper;
    private final DishIngredientFormDTOToDishIngredientMapper dishIngredientFormDTOToDishIngredientMapper;
    private final DishIngredientToDishIngredientDTOMapper dishIngredientToDishIngredientDTOMapper;
    private final ImageUtil imageUtil;
    private final UploadUtil uploadUtil;

    public DishDTO create(Category category, List<DishIngredient> dishIngredientList, DishFormDTO dishFormDTO) throws IOException {
        Dish dishToBeSaved = convertDishFormDTOToDish(dishFormDTO);
        dishToBeSaved.setCategory(category);

        MultipartFile dishImage = imageUtil.convertBase64ToMultipartFile(dishFormDTO.image(), "dish_image.png");
        uploadDishImage(dishToBeSaved, dishImage);

        Dish dishSaved = dishRepository.save(dishToBeSaved);
        saveDishIngredientList(dishIngredientList, dishSaved);
        return convertDishToDishDTO(dishSaved);
    }

    private void uploadDishImage(Dish dish, MultipartFile dishImage) {
        try {
            if (uploadUtil.makeImageUpload(dishImage)) {
                dish.setImage(dishImage.getOriginalFilename());
            }
        } catch (Exception e) {
            throw new UploadImageException("Erro ao realizar upload da imagem do prato.");
        }
    }

    public DishDTO convertDishToDishDTO(Dish dish) {
        List<DishIngredient> dishIngredientList = dishIngredientRepository.findAllByDish(dish);
        List<DishIngredientDTO> dishIngredientDTOList = convertDishIngredientToDishIngredientDTO(dishIngredientList);
        String encodedImage = imageUtil.encodeImageToBase64(dish.getImage());

        DishDTO dishDTO = dishToDishDTOMapper.convert(dish, dishIngredientDTOList, encodedImage);

        return dishDTO;
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

    public DishDTO update(Dish dish, Category category, List<DishIngredient> dishIngredientList, DishFormDTO dishFormDTO) throws IOException {
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
        if(dishFormDTO.image() != null && !dishFormDTO.image().isBlank()) {
            imageUtil.removeOldImage(dish.getImage());
            MultipartFile newImage = imageUtil.convertBase64ToMultipartFile(dishFormDTO.image(), "dish_image.png");
            uploadDishImage(dish, newImage);
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
        dishIngredientRepository.deleteAll(existingDishIngredients);

        for (DishIngredient newIngredient : newDishIngredientList) {
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

    public void verifyNoDuplicatedIngredients(List<DishIngredientFormDTO> dishIngredientFormDTOList) {
        Set<Long> ingredientIds = new HashSet<>();
        for (DishIngredientFormDTO dishIngredientFormDTO : dishIngredientFormDTOList) {
            if (!ingredientIds.add(dishIngredientFormDTO.ingredientId())) {
                throw new DuplicateKeyException("O ingrediente com ID " + dishIngredientFormDTO.ingredientId() + " foi informado mais de uma vez.");
            }
        }
    }
}
