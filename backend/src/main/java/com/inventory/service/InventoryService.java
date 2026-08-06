package com.inventory.service;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryStatsDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Category;
import com.inventory.model.Inventory;
import com.inventory.model.InventoryStatus;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final BarcodeService barcodeService;

    public InventoryService(InventoryRepository inventoryRepository, CategoryRepository categoryRepository,
                             CategoryService categoryService, BarcodeService barcodeService) {
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.barcodeService = barcodeService;
    }

    public List<InventoryDTO> findAll() {
        return inventoryRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Lets a department see what's currently available in a category (name, remaining
    // quantity, specs) before deciding what to request.
    public List<InventoryDTO> findByCategory(UUID categoryId) {
        categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return inventoryRepository.findByCategoryIdAndDeletedFalseOrderByCreatedAtDesc(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public InventoryDTO findByBarcode(String barcode) {
        return toDto(inventoryRepository.findByBarcodeAndDeletedFalse(normalizeBarcode(barcode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
    }

    // Non-throwing variant used by the unified barcode scan endpoint, which needs to
    // try "is this an inventory barcode?" before falling back to "is this an
    // allocation barcode?" without exceptions as control flow between the two.
    public Optional<InventoryDTO> tryFindByBarcode(String barcode) {
        return inventoryRepository.findByBarcodeAndDeletedFalse(normalizeBarcode(barcode)).map(this::toDto);
    }

    public InventoryStatsDTO stats() {
        return new InventoryStatsDTO(
                inventoryRepository.countByDeletedFalse(),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAllocatedQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityLessThanEqual(5)
        );
    }

    @Transactional
    public InventoryDTO create(InventoryDTO dto) {
        validateQuantities(dto);
        Inventory inventory = new Inventory();
        apply(dto, inventory);

        String barcode = resolveBarcodeForCreate(dto, inventory.getCategory().getId());
        inventory.setBarcode(barcode);
        inventory.setBarcodeImageUrl(barcodeService.storeBarcodeImage(barcode));

        inventory.setAvailableQuantity(dto.getAvailableQuantity() > 0 || dto.getAllocatedQuantity() > 0
                ? dto.getAvailableQuantity()
                : dto.getQuantity());
        inventory.setAllocatedQuantity(Math.max(dto.getAllocatedQuantity(), 0));
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? InventoryStatus.ALLOCATED : InventoryStatus.AVAILABLE);
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryDTO update(UUID id, InventoryDTO dto) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        validateQuantities(dto);
        apply(dto, inventory);
        // Barcode is assigned once at creation and is intentionally immutable afterwards -
        // it's derived from the category's sequence counter, so it can't be re-derived later.
        inventory.setAvailableQuantity(dto.getAvailableQuantity());
        inventory.setAllocatedQuantity(dto.getAllocatedQuantity());
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? InventoryStatus.ALLOCATED : InventoryStatus.AVAILABLE);
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public void softDelete(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        inventory.setDeleted(true);
        inventoryRepository.save(inventory);
    }

    private String resolveBarcodeForCreate(InventoryDTO dto, UUID categoryId) {
        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()) {
            // Admin supplied an explicit code (e.g. re-labelling a physical asset that
            // already has a barcode from elsewhere) - honor it, but it still has to be unique.
            String provided = normalizeBarcode(dto.getBarcode());
            if (inventoryRepository.existsByBarcode(provided)) {
                throw new BadRequestException("Barcode '" + provided + "' is already assigned to another item");
            }
            return provided;
        }
        // Normal path: an incrementing number scoped to the item's category, e.g. "ELEC-0007".
        // categoryService.nextInventoryBarcode() locks the category row and increments its
        // counter atomically, so this is guaranteed unique without any retry loop.
        String barcode = categoryService.nextInventoryBarcode(categoryId);
        if (inventoryRepository.existsByBarcode(barcode)) {
            // Should be unreachable under normal operation (the counter guarantees a fresh
            // number every time) - only possible if the DB was hand-edited. Fail loudly
            // rather than silently reusing someone else's barcode.
            throw new IllegalStateException("Generated barcode '" + barcode + "' already exists - check for manually inserted inventory rows");
        }
        return barcode;
    }

    private String normalizeBarcode(String barcode) {
        return barcode == null ? null : barcode.trim().toUpperCase();
    }

    private void apply(InventoryDTO dto, Inventory inventory) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        inventory.setCategory(category);
        inventory.setInventoryName(dto.getInventoryName());
        inventory.setDescription(dto.getDescription());
        inventory.setQuantity(dto.getQuantity());
        inventory.setSerialNumber(dto.getSerialNumber());
        inventory.setCondition(dto.getCondition());
    }

    private void validateQuantities(InventoryDTO dto) {
        if (dto.getQuantity() < 0 || dto.getAvailableQuantity() < 0 || dto.getAllocatedQuantity() < 0) {
            throw new BadRequestException("Quantities cannot be negative");
        }
        if (dto.getAvailableQuantity() + dto.getAllocatedQuantity() > dto.getQuantity()) {
            throw new BadRequestException("Available + allocated cannot exceed total quantity");
        }
    }

    public InventoryDTO toDto(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setBarcode(inventory.getBarcode());
        dto.setInventoryName(inventory.getInventoryName());
        if (inventory.getCategory() != null) {
            dto.setCategoryId(inventory.getCategory().getId());
            dto.setCategoryName(inventory.getCategory().getName());
            dto.setCategoryCode(inventory.getCategory().getCode());
        }
        dto.setDescription(inventory.getDescription());
        dto.setQuantity(inventory.getQuantity());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setAllocatedQuantity(inventory.getAllocatedQuantity());
        dto.setSerialNumber(inventory.getSerialNumber());
        dto.setCondition(inventory.getCondition());
        dto.setStatus(inventory.getStatus().name());
        dto.setBarcodeImageUrl(inventory.getBarcodeImageUrl());
        dto.setCreatedAt(inventory.getCreatedAt());
        return dto;
    }
}
