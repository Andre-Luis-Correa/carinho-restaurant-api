package com.menumaster.restaurant.category.service;

import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryFormDTO;
import com.menumaster.restaurant.category.domain.model.Category;
import com.menumaster.restaurant.category.mapper.CategoryFormDTOToCategoryMapper;
import com.menumaster.restaurant.category.mapper.CategoryToCategoryDTOMapper;
import com.menumaster.restaurant.category.repository.CategoryRepository;
import com.menumaster.restaurant.exception.type.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryFormDTOToCategoryMapper categoryFormDTOToCategoryMapper;
    private final CategoryToCategoryDTOMapper categoryToCategoryDTOMapper;

    public CategoryDTO create(CategoryFormDTO categoryFormDTO) {
        Category categoryToBeSaved = convertCategoryFormDTOToCategory(categoryFormDTO);
        Category categorySaved = categoryRepository.save(categoryToBeSaved);
        return convertCategoryToCategoryDTO(categorySaved);
    }

    public Category convertCategoryFormDTOToCategory(CategoryFormDTO categoryFormDTO) {
        return categoryFormDTOToCategoryMapper.convert(categoryFormDTO);
    }

    public CategoryDTO convertCategoryToCategoryDTO(Category category) {
        return categoryToCategoryDTOMapper.convert(category);
    }

    public Category getOrThrowException(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("category", id.toString()));
    }

    public CategoryDTO update(Category category, CategoryFormDTO categoryFormDTO) {
        if(  categoryFormDTO.name() != null && !category.getName().isBlank()) {
            category.setName(categoryFormDTO.name());
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertCategoryToCategoryDTO(updatedCategory);
    }

    public void delete(Category category) {
        categoryRepository.delete(category);
    }


    public List<CategoryDTO> list() {
        List<Category> categoryList = categoryRepository.findAll();
        return categoryList.stream().map(categoryToCategoryDTOMapper::convert).toList();
    }

    public Page<CategoryDTO> listByPageable(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        return categoryPage.map(categoryToCategoryDTOMapper::convert);
    }
}
