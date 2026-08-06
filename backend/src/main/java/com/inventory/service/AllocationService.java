package com.inventory.service;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.AllocationRequest;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Allocation;
import com.inventory.model.AllocationStatus;
import com.inventory.model.Department;
import com.inventory.model.Inventory;
import com.inventory.model.InventoryHistory;
import com.inventory.model.InventoryStatus;
import com.inventory.model.User;
import com.inventory.repository.AllocationRepository;
import com.inventory.repository.DepartmentRepository;
import com.inventory.repository.InventoryHistoryRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AllocationService {
    private final InventoryRepository inventoryRepository;
    private final AllocationRepository allocationRepository;
    private final InventoryHistoryRepository historyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    private final BarcodeService barcodeService;

    public AllocationService(InventoryRepository inventoryRepository, AllocationRepository allocationRepository,
                             InventoryHistoryRepository historyRepository, DepartmentRepository departmentRepository,
                             UserService userService, BarcodeService barcodeService) {
        this.inventoryRepository = inventoryRepository;
        this.allocationRepository = allocationRepository;
        this.historyRepository = historyRepository;
        this.departmentRepository = departmentRepository;
        this.userService = userService;
        this.barcodeService = barcodeService;
    }

    // Inventory carries @Version, so if two allocations race against the same item's
    // available quantity, the second save() throws ObjectOptimisticLockingFailureException
    // (mapped to a clean 409 by GlobalExceptionHandler) instead of silently over-allocating.
    public AllocationDTO allocate(AllocationRequest request) {
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (inventory.isDeleted()) {
            throw new BadRequestException("Inventory item is deleted");
        }
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        inventory.setAllocatedQuantity(inventory.getAllocatedQuantity() + request.getQuantity());
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? InventoryStatus.ALLOCATED : InventoryStatus.AVAILABLE);
        inventoryRepository.save(inventory);

        User user = userService.getCurrentUser();
        Allocation allocation = new Allocation();
        allocation.setInventory(inventory);
        allocation.setDepartment(department);
        allocation.setQuantity(request.getQuantity());
        allocation.setAllocatedBy(user);
        allocation.setStatus(AllocationStatus.CONFIRMED);

        // Composite, printable barcode: year-categoryCode-inventoryBarcode-departmentCode,
        // plus a short unique suffix (see the field comment on Allocation.allocationBarcode
        // for why the suffix is necessary).
        String allocationBarcode = buildAllocationBarcode(inventory, department);
        allocation.setAllocationBarcode(allocationBarcode);
        allocation.setAllocationBarcodeImageUrl(barcodeService.storeBarcodeImage(allocationBarcode));

        allocationRepository.save(allocation);

        InventoryHistory history = new InventoryHistory();
        history.setInventory(inventory);
        history.setActionType("ALLOCATION");
        history.setQuantity(request.getQuantity());
        history.setPerformedBy(user);
        historyRepository.save(history);

        return toDto(allocation);
    }

    private String buildAllocationBarcode(Inventory inventory, Department department) {
        String year = String.valueOf(Year.now().getValue());
        String categoryCode = inventory.getCategory() != null ? inventory.getCategory().getCode() : "NA";
        String departmentCode = department.getDepartmentCode();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.join("-", year, categoryCode, inventory.getBarcode(), departmentCode, uniqueSuffix);
    }

    public List<AllocationDTO> findAll() {
        return allocationRepository.findAllByOrderByAllocatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AllocationDTO> findByDepartment(UUID departmentId) {
        return allocationRepository.findByDepartmentIdOrderByAllocatedAtDesc(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Non-throwing lookup used by the unified barcode scan endpoint, which tries an
    // inventory-barcode match first and only falls back to this if that misses.
    public Optional<AllocationDTO> tryFindByAllocationBarcode(String allocationBarcode) {
        return allocationRepository.findByAllocationBarcode(allocationBarcode).map(this::toDto);
    }

    private AllocationDTO toDto(Allocation allocation) {
        AllocationDTO dto = new AllocationDTO();
        dto.setId(allocation.getId());
        dto.setInventoryId(allocation.getInventory().getId());
        dto.setInventoryName(allocation.getInventory().getInventoryName());
        dto.setDepartmentId(allocation.getDepartment().getId());
        dto.setDepartmentName(allocation.getDepartment().getDepartmentName());
        dto.setQuantity(allocation.getQuantity());
        dto.setStatus(allocation.getStatus().name());
        dto.setAllocatedAt(allocation.getAllocatedAt());
        dto.setAllocationBarcode(allocation.getAllocationBarcode());
        dto.setAllocationBarcodeImageUrl(allocation.getAllocationBarcodeImageUrl());
        return dto;
    }
}
