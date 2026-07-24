package com.possystem.service;

import com.possystem.dto.CategoryRequest;
import com.possystem.dto.CategoryResponse;
import com.possystem.dto.CategoryStatusRequest;
import com.possystem.entity.Category;
import com.possystem.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {

        String categoryName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(categoryName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category with this name already exists"
            );
        }

        Category category = Category.builder()
                .name(categoryName)
                .description(cleanDescription(request.getDescription()))
                .status("ACTIVE")
                .build();

        Category savedCategory = categoryRepository.save(category);

        return mapToCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {

        Category category = findCategoryById(id);

        return mapToCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        Category category = findCategoryById(id);

        String updatedName = request.getName().trim();

        Optional<Category> existingCategory =
                categoryRepository.findByNameIgnoreCase(updatedName);

        if (existingCategory.isPresent()
                && !existingCategory.get().getId().equals(category.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another category with this name already exists"
            );
        }

        category.setName(updatedName);
        category.setDescription(cleanDescription(request.getDescription()));

        Category updatedCategory = categoryRepository.save(category);

        return mapToCategoryResponse(updatedCategory);
    }

    @Transactional
    public CategoryResponse updateCategoryStatus(
            Long id,
            CategoryStatusRequest request
    ) {

        Category category = findCategoryById(id);

        String newStatus = request.getStatus()
                .trim()
                .toUpperCase(Locale.ROOT);

        category.setStatus(newStatus);

        Category updatedCategory = categoryRepository.save(category);

        return mapToCategoryResponse(updatedCategory);
    }
    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found with ID: " + id
                ));
    }

    private CategoryResponse mapToCategoryResponse(Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String cleanDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
