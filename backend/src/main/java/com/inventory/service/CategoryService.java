package com.inventory.service;

import com.inventory.dto.CategoryDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.InventoryRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryRequestRepository inventoryRequestRepository;

    public CategoryService(CategoryRepository categoryRepository,
                            InventoryRepository inventoryRepository,
                            InventoryRequestRepository inventoryRequestRepository) {
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryRequestRepository = inventoryRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category get(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional
    public CategoryDTO create(CategoryDTO dto) {
        String name = dto.getName() == null ? null : dto.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new BadRequestException("Category name is required");
        }
        String code = dto.getCode() == null ? null : dto.getCode().trim().toUpperCase();
        if (code == null || code.isEmpty()) {
            throw new BadRequestException("Category code is required");
        }
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A category with this name already exists");
        }
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("A category with this code already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setCode(code);
        category.setDescription(dto.getDescription());
        return toDto(categoryRepository.save(category));
    }

    // NOTE: the group-barcode sequence increment used to live here as
    // nextInventoryBarcode(), called from InventoryService.create(). It's now inlined
    // directly in InventoryService, which already holds a pessimistic lock on the
    // Category row for its whole create-or-merge-into-existing-group decision (needed
    // to prevent duplicate groups under concurrent registration), so reusing that same
    // lock there avoids a second, redundant SELECT FOR UPDATE on the same row.

    @Transactional
    public void delete(UUID id) {
        Category category = get(id);
        if (inventoryRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete a category that still has inventory items assigned to it");
        }
        if (inventoryRequestRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete a category that still has inventory requests assigned to it");
        }
        categoryRepository.delete(category);
    }

    private CategoryDTO toDto(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCode(category.getCode());
        dto.setDescription(category.getDescription());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
