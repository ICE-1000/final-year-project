package com.inventory.service;

import com.inventory.dto.InventoryBatchDTO;
import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryStatsDTO;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Category;
import com.inventory.model.Inventory;
import com.inventory.model.InventoryBatch;
import com.inventory.model.InventoryStatus;
import com.inventory.model.InventoryUnit;
import com.inventory.model.User;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryBatchRepository;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.InventoryUnitRepository;
import com.inventory.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryUnitRepository unitRepository;
    private final InventoryBatchRepository batchRepository;
    private final BarcodeService barcodeService;
    private final UserService userService;

    public InventoryService(InventoryRepository inventoryRepository, CategoryRepository categoryRepository,
                             InventoryUnitRepository unitRepository, InventoryBatchRepository batchRepository,
                             BarcodeService barcodeService, UserService userService) {
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.batchRepository = batchRepository;
        this.barcodeService = barcodeService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findAll() {
        return inventoryRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findByCategory(UUID categoryId) {
        categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return inventoryRepository.findByCategoryIdAndDeletedFalseOrderByCreatedAtDesc(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDTO findByBarcode(String barcode) {
        return toDto(inventoryRepository.findByBarcodeAndDeletedFalse(normalizeBarcode(barcode))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
    }

    @Transactional(readOnly = true)
    public Optional<InventoryDTO> tryFindByBarcode(String barcode) {
        return inventoryRepository.findByBarcodeAndDeletedFalse(normalizeBarcode(barcode)).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public InventoryDTO getById(UUID id) {
        return toDto(inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found")));
    }

    public InventoryStatsDTO stats() {
        return new InventoryStatsDTO(
                inventoryRepository.countByDeletedFalse(),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAllocatedQuantityGreaterThan(0),
                inventoryRepository.countByDeletedFalseAndAvailableQuantityLessThanEqual(Constants.LOW_STOCK_THRESHOLD)
        );
    }

    // Creates a brand-new item group, OR - if a group already exists in this category
    // with the same name/brand/specification (case-insensitive) - adds this many new
    // units to that existing group instead, continuing its unit-number sequence rather
    // than starting over. The whole decision + any resulting sequence increments happen
    // under a pessimistic lock on the parent Category row, so two concurrent
    // registrations for what would be the same brand-new group can never both think "no
    // match exists" and create two duplicate groups.
    @Transactional
    public InventoryDTO create(InventoryDTO dto) {
        if (dto.getCategoryId() == null) {
            throw new BadRequestException("Category is required");
        }
        Category category = categoryRepository.findByIdForUpdate(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String name = requireText(dto.getInventoryName(), "Item name is required");
        String brand = normalizeOptional(dto.getBrand());
        String specification = normalizeOptional(dto.getSpecification());
        int quantityToAdd = dto.getQuantity();
        if (quantityToAdd < 0) {
            throw new BadRequestException("Quantity cannot be negative");
        }

        Optional<Inventory> existing = inventoryRepository.findMatchingGroup(category.getId(), name, brand, specification);

        if (existing.isPresent()) {
            if (dto.getBarcode() != null && !dto.getBarcode().isBlank()) {
                throw new BadRequestException("Cannot set a custom barcode when adding stock to an existing item "
                        + "- the existing item's barcode is used automatically for the new units.");
            }
            if (quantityToAdd < 1) {
                throw new BadRequestException("Quantity to add must be at least 1");
            }
            Inventory group = existing.get();
            if (dto.getDescription() != null) group.setDescription(dto.getDescription());
            if (dto.getCondition() != null) group.setCondition(dto.getCondition());
            InventoryBatch batch = createBatch(category, group, quantityToAdd);
            generateUnits(group, quantityToAdd, batch);
            group.setQuantity(group.getQuantity() + quantityToAdd);
            group.setAvailableQuantity(group.getAvailableQuantity() + quantityToAdd);
            group.setStatus(InventoryStatus.AVAILABLE);
            return toDto(inventoryRepository.save(group));
        }

        Inventory inventory = new Inventory();
        inventory.setCategory(category);
        inventory.setInventoryName(name);
        inventory.setBrand(brand);
        inventory.setSpecification(specification);
        inventory.setDescription(dto.getDescription());
        inventory.setSerialNumber(dto.getSerialNumber());
        inventory.setCondition(dto.getCondition());
        inventory.setQuantity(quantityToAdd);
        inventory.setAvailableQuantity(quantityToAdd);
        inventory.setAllocatedQuantity(0);
        inventory.setNextUnitSequence(1L);
        inventory.setStatus(InventoryStatus.AVAILABLE);

        String barcode = resolveBarcodeForCreate(dto, category);
        inventory.setBarcode(barcode);
        inventory.setBarcodeImageUrl(barcodeService.storeBarcodeImage(barcode));

        Inventory saved = inventoryRepository.save(inventory);
        if (quantityToAdd > 0) {
            InventoryBatch batch = createBatch(category, saved, quantityToAdd);
            generateUnits(saved, quantityToAdd, batch);
        }
        return toDto(saved);
    }

    // Metadata-only correction (name/brand/specification/description/condition/serial).
    // Quantity, barcode, and category are intentionally NOT editable here: quantity only
    // changes by registering more stock (see create() above, which merges into a
    // matching group), and barcode/category are immutable once units have been minted
    // against them.
    @Transactional
    public InventoryDTO update(UUID id, InventoryDTO dto) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        if (dto.getInventoryName() != null) {
            inventory.setInventoryName(requireText(dto.getInventoryName(), "Item name is required"));
        }
        inventory.setBrand(normalizeOptional(dto.getBrand()));
        inventory.setSpecification(normalizeOptional(dto.getSpecification()));
        inventory.setDescription(dto.getDescription());
        inventory.setSerialNumber(dto.getSerialNumber());
        inventory.setCondition(dto.getCondition());
        return toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public void softDelete(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        inventory.setDeleted(true);
        inventoryRepository.save(inventory);
    }

    // --- Unit-level reads, used by the scan endpoint and the "view/print barcodes" UI ---

    @Transactional(readOnly = true)
    public Optional<InventoryUnitDTO> tryFindUnitByBarcode(String barcode) {
        String normalized = barcode == null ? null : barcode.trim().toUpperCase();
        return unitRepository.findByUnitBarcodeWithDetails(normalized).map(this::toUnitDto);
    }

    @Transactional(readOnly = true)
    public List<InventoryUnitDTO> findUnitsByAllocation(UUID allocationId) {
        return unitRepository.findByAllocationIdWithDetails(allocationId).stream()
                .map(this::toUnitDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryUnitDTO> findUnitsByInventory(UUID inventoryId) {
        inventoryRepository.findById(inventoryId).orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        return unitRepository.findByInventoryIdWithDetailsOrderByUnitNumberAsc(inventoryId).stream()
                .map(this::toUnitDto)
                .collect(Collectors.toList());
    }

    // Full registration history for a group - every batch (registration event) that
    // ever added stock to it, newest first.
    @Transactional(readOnly = true)
    public List<InventoryBatchDTO> findBatchesByInventory(UUID inventoryId) {
        inventoryRepository.findById(inventoryId).orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        return batchRepository.findByInventoryIdWithDetails(inventoryId).stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    // --- internal helpers ---

    // Generates `count` new units for `group`, continuing its unit-number sequence, and
    // links each to `batch` (the registration event that produced them). Caller must
    // already hold the pessimistic lock on the parent Category (see create()) so this
    // is safe from races without a separate lock here.
    private void generateUnits(Inventory group, int count, InventoryBatch batch) {
        long seq = group.getNextUnitSequence() == null ? 1L : group.getNextUnitSequence();
        List<InventoryUnit> units = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String unitBarcode = group.getBarcode() + "-" + String.format("%04d", seq);
            InventoryUnit unit = new InventoryUnit();
            unit.setInventory(group);
            unit.setUnitNumber((int) seq);
            unit.setUnitBarcode(unitBarcode);
            unit.setUnitBarcodeImageUrl(barcodeService.storeBarcodeImage(unitBarcode));
            unit.setStatus(InventoryStatus.AVAILABLE);
            unit.setBatch(batch);
            units.add(unit);
            seq++;
        }
        group.setNextUnitSequence(seq);
        unitRepository.saveAll(units);
    }

    // Records one registration event (new group or adding stock to an existing one) as
    // its own InventoryBatch row, with a human-readable code like "2026-ELEC-0002"
    // (year-categoryCode-sequence). Caller must already hold the lock on `lockedCategory`
    // (see create()), which is what serializes this sequence increment safely.
    private InventoryBatch createBatch(Category lockedCategory, Inventory group, int quantity) {
        long seq = lockedCategory.getNextBatchSequence() == null ? 1L : lockedCategory.getNextBatchSequence();
        lockedCategory.setNextBatchSequence(seq + 1);
        categoryRepository.save(lockedCategory);

        InventoryBatch batch = new InventoryBatch();
        batch.setBatchCode(Year.now().getValue() + "-" + lockedCategory.getCode() + "-" + String.format("%04d", seq));
        batch.setInventory(group);
        batch.setQuantityRegistered(quantity);
        batch.setRegisteredBy(userService.getCurrentUser());
        return batchRepository.save(batch);
    }

    private String resolveBarcodeForCreate(InventoryDTO dto, Category lockedCategory) {
        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()) {
            String provided = normalizeBarcode(dto.getBarcode());
            if (inventoryRepository.existsByBarcode(provided)) {
                throw new BadRequestException("Barcode '" + provided + "' is already assigned to another item");
            }
            return provided;
        }
        long sequence = lockedCategory.getNextInventorySequence();
        lockedCategory.setNextInventorySequence(sequence + 1);
        categoryRepository.save(lockedCategory);
        String barcode = lockedCategory.getCode() + "-" + String.format("%04d", sequence);
        if (inventoryRepository.existsByBarcode(barcode)) {
            throw new IllegalStateException("Generated barcode '" + barcode + "' already exists - check for manually inserted inventory rows");
        }
        return barcode;
    }

    private String normalizeBarcode(String barcode) {
        return barcode == null ? null : barcode.trim().toUpperCase();
    }

    private String requireText(String value, String errorMessage) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BadRequestException(errorMessage);
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        dto.setBrand(inventory.getBrand());
        dto.setSpecification(inventory.getSpecification());
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

    private InventoryUnitDTO toUnitDto(InventoryUnit unit) {
        InventoryUnitDTO dto = new InventoryUnitDTO();
        dto.setId(unit.getId());
        dto.setUnitNumber(unit.getUnitNumber());
        dto.setUnitBarcode(unit.getUnitBarcode());
        dto.setUnitBarcodeImageUrl(unit.getUnitBarcodeImageUrl());
        dto.setStatus(unit.getStatus().name());
        dto.setCreatedAt(unit.getCreatedAt());
        if (unit.getBatch() != null) {
            dto.setBatchCode(unit.getBatch().getBatchCode());
        }
        Inventory inv = unit.getInventory();
        if (inv != null) {
            dto.setInventoryId(inv.getId());
            dto.setInventoryName(inv.getInventoryName());
            dto.setBrand(inv.getBrand());
            dto.setSpecification(inv.getSpecification());
            dto.setDescription(inv.getDescription());
            dto.setCondition(inv.getCondition());
            if (inv.getCategory() != null) {
                dto.setCategoryId(inv.getCategory().getId());
                dto.setCategoryName(inv.getCategory().getName());
                dto.setCategoryCode(inv.getCategory().getCode());
            }
        }
        if (unit.getAllocation() != null) {
            dto.setAllocationId(unit.getAllocation().getId());
            dto.setAllocatedAt(unit.getAllocation().getAllocatedAt());
            if (unit.getAllocation().getDepartment() != null) {
                dto.setDepartmentId(unit.getAllocation().getDepartment().getId());
                dto.setDepartmentName(unit.getAllocation().getDepartment().getDepartmentName());
            }
        }
        return dto;
    }

    private InventoryBatchDTO toBatchDto(InventoryBatch batch) {
        InventoryBatchDTO dto = new InventoryBatchDTO();
        dto.setId(batch.getId());
        dto.setBatchCode(batch.getBatchCode());
        dto.setQuantityRegistered(batch.getQuantityRegistered());
        dto.setRegisteredAt(batch.getRegisteredAt());
        if (batch.getInventory() != null) {
            dto.setInventoryId(batch.getInventory().getId());
            dto.setInventoryName(batch.getInventory().getInventoryName());
        }
        if (batch.getRegisteredBy() != null) {
            dto.setRegisteredByUsername(batch.getRegisteredBy().getUsername());
        }
        return dto;
    }
}
