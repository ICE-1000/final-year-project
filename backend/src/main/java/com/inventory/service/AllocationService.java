package com.inventory.service;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.AllocationRequest;
import com.inventory.dto.InventoryUnitDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Allocation;
import com.inventory.model.AllocationStatus;
import com.inventory.model.Department;
import com.inventory.model.Inventory;
import com.inventory.model.InventoryHistory;
import com.inventory.model.InventoryStatus;
import com.inventory.model.InventoryUnit;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.AllocationRepository;
import com.inventory.repository.DepartmentRepository;
import com.inventory.repository.InventoryHistoryRepository;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.InventoryUnitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AllocationService {
    private final InventoryRepository inventoryRepository;
    private final InventoryUnitRepository unitRepository;
    private final AllocationRepository allocationRepository;
    private final InventoryHistoryRepository historyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    public AllocationService(InventoryRepository inventoryRepository, InventoryUnitRepository unitRepository,
                             AllocationRepository allocationRepository, InventoryHistoryRepository historyRepository,
                             DepartmentRepository departmentRepository, UserService userService,
                             InventoryService inventoryService) {
        this.inventoryRepository = inventoryRepository;
        this.unitRepository = unitRepository;
        this.allocationRepository = allocationRepository;
        this.historyRepository = historyRepository;
        this.departmentRepository = departmentRepository;
        this.userService = userService;
        this.inventoryService = inventoryService;
    }

    // Pulls exactly `request.getQuantity()` specific, individually-barcoded units out
    // of the group (oldest-registered first), row-locked so two concurrent allocation
    // requests against the same group can never both grab the same physical unit - this
    // is real per-row availability, not just a counter check, so it can never
    // over-allocate even under a race. Each selected unit is linked to the new
    // Allocation event, which is what powers "view barcodes for this allocation".
    public AllocationDTO allocate(AllocationRequest request) {
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (inventory.isDeleted()) {
            throw new BadRequestException("Inventory item is deleted");
        }
        if (request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        List<InventoryUnit> units = unitRepository.findAvailableUnitsForUpdate(
                inventory.getId(), PageRequest.of(0, request.getQuantity()));
        if (units.size() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock: only " + units.size() + " unit(s) available");
        }

        User user = userService.getCurrentUser();
        Allocation allocation = new Allocation();
        allocation.setInventory(inventory);
        allocation.setDepartment(department);
        allocation.setQuantity(units.size());
        allocation.setAllocatedBy(user);
        allocation.setStatus(AllocationStatus.CONFIRMED);
        allocationRepository.save(allocation);

        for (InventoryUnit unit : units) {
            unit.setStatus(InventoryStatus.ALLOCATED);
            unit.setAllocation(allocation);
        }
        unitRepository.saveAll(units);

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - units.size());
        inventory.setAllocatedQuantity(inventory.getAllocatedQuantity() + units.size());
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? InventoryStatus.ALLOCATED : InventoryStatus.AVAILABLE);
        inventoryRepository.save(inventory);

        InventoryHistory history = new InventoryHistory();
        history.setInventory(inventory);
        history.setActionType("ALLOCATION");
        history.setQuantity(units.size());
        history.setPerformedBy(user);
        historyRepository.save(history);

        return toDto(allocation);
    }

    @Transactional(readOnly = true)
    public List<AllocationDTO> findAll() {
        return allocationRepository.findAllByOrderByAllocatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AllocationDTO> findByDepartment(UUID departmentId) {
        return allocationRepository.findByDepartmentIdOrderByAllocatedAtDesc(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Powers the "view barcodes for this allocation" table: returns every individually-
    // barcoded unit that was part of this allocation event, each with its own
    // downloadable barcode image. A DEPARTMENT caller may only view their own
    // department's allocations; ADMIN may view any.
    @Transactional(readOnly = true)
    public List<InventoryUnitDTO> getUnitsForAllocation(UUID allocationId, User currentUser) {
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found"));
        if (currentUser.getRole() != Role.ADMIN) {
            UUID ownDeptId = currentUser.getDepartment() == null ? null : currentUser.getDepartment().getId();
            UUID allocDeptId = allocation.getDepartment() == null ? null : allocation.getDepartment().getId();
            if (ownDeptId == null || !ownDeptId.equals(allocDeptId)) {
                throw new AccessDeniedException("You can only view your own department's allocation units");
            }
        }
        return inventoryService.findUnitsByAllocation(allocationId);
    }

    private AllocationDTO toDto(Allocation allocation) {
        AllocationDTO dto = new AllocationDTO();
        dto.setId(allocation.getId());
        if (allocation.getInventory() != null) {
            dto.setInventoryId(allocation.getInventory().getId());
            dto.setInventoryName(allocation.getInventory().getInventoryName());
        }
        if (allocation.getDepartment() != null) {
            dto.setDepartmentId(allocation.getDepartment().getId());
            dto.setDepartmentName(allocation.getDepartment().getDepartmentName());
        }
        dto.setQuantity(allocation.getQuantity());
        dto.setStatus(allocation.getStatus().name());
        dto.setAllocatedAt(allocation.getAllocatedAt());
        return dto;
    }
}
