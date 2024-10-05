package com.menumaster.restaurant.category.controller;

import com.menumaster.restaurant.category.domain.dto.CategoryDTO;
import com.menumaster.restaurant.category.domain.dto.CategoryFormDTO;
import com.menumaster.restaurant.category.domain.model.Category;
import com.menumaster.restaurant.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryFormDTO categoryFormDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(categoryFormDTO));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CategoryDTO> update(@PathVariable Long id, @RequestBody CategoryFormDTO categoryFormDTO) {
        Category category = categoryService.getOrThrowException(id);
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.update(category, categoryFormDTO));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Category category = categoryService.getOrThrowException(id);
        categoryService.delete(category);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<CategoryDTO>> getCategoryDTOList() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.list());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CategoryDTO>> getCategoryDTOPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.listByPageable(pageable));
    }

}
