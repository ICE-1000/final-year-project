package com.inventory.service;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.AllocationRequest;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Allocation;
import com.inventory.model.Department;
import com.inventory.model.Inventory;
import com.inventory.model.InventoryHistory;
import com.inventory.model.User;
import com.inventory.repository.AllocationRepository;
import com.inventory.repository.DepartmentRepository;
import com.inventory.repository.InventoryHistoryRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public AllocationService(InventoryRepository inventoryRepository, AllocationRepository allocationRepository,
                             InventoryHistoryRepository historyRepository, DepartmentRepository departmentRepository,
                             UserService userService) {
        this.inventoryRepository = inventoryRepository;
        this.allocationRepository = allocationRepository;
        this.historyRepository = historyRepository;
        this.departmentRepository = departmentRepository;
        this.userService = userService;
    }

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
        inventory.setStatus(inventory.getAvailableQuantity() == 0 ? "ALLOCATED" : "AVAILABLE");
        inventoryRepository.save(inventory);

        User user = userService.getCurrentUser();
        Allocation allocation = new Allocation();
        allocation.setInventory(inventory);
        allocation.setDepartment(department);
        allocation.setQuantity(request.getQuantity());
        allocation.setAllocatedBy(user);
        allocation.setStatus("CONFIRMED");
        allocationRepository.save(allocation);

        InventoryHistory history = new InventoryHistory();
        history.setInventory(inventory);
        history.setActionType("ALLOCATION");
        history.setQuantity(request.getQuantity());
        history.setPerformedBy(user);
        historyRepository.save(history);

        return toDto(allocation);
    }

    public List<AllocationDTO> findAll() {
        return allocationRepository.findAllByOrderByAllocatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AllocationDTO> findByDepartment(UUID departmentId) {
        return allocationRepository.findByDepartmentIdOrderByAllocatedAtDesc(departmentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AllocationDTO toDto(Allocation allocation) {
        AllocationDTO dto = new AllocationDTO();
        dto.setId(allocation.getId());
        dto.setInventoryId(allocation.getInventory().getId());
        dto.setInventoryName(allocation.getInventory().getInventoryName());
        dto.setDepartmentId(allocation.getDepartment().getId());
        dto.setDepartmentName(allocation.getDepartment().getDepartmentName());
        dto.setQuantity(allocation.getQuantity());
        dto.setStatus(allocation.getStatus());
        dto.setAllocatedAt(allocation.getAllocatedAt());
        return dto;
    }
}
