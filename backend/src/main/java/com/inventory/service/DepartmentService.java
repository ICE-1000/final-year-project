package com.inventory.service;

import com.inventory.dto.DepartmentDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Department;
import com.inventory.repository.AllocationRepository;
import com.inventory.repository.DepartmentRepository;
import com.inventory.repository.InventoryRequestRepository;
import com.inventory.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AllocationRepository allocationRepository;
    private final InventoryRequestRepository inventoryRequestRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              AllocationRepository allocationRepository,
                              InventoryRequestRepository inventoryRequestRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.allocationRepository = allocationRepository;
        this.inventoryRequestRepository = inventoryRequestRepository;
    }

    public List<DepartmentDTO> findAll() {
        return departmentRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public DepartmentDTO create(DepartmentDTO dto) {
        Department department = new Department();
        department.setDepartmentName(dto.getDepartmentName());
        department.setDepartmentCode(dto.getDepartmentCode());
        return toDto(departmentRepository.save(department));
    }

    public Department get(UUID id) {
        return departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    @Transactional
    public void delete(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        // Referential-integrity guard: refuse the delete with a clear message instead of
        // letting it either fail with a raw DB constraint error or orphan dependent records.
        if (userRepository.existsByDepartmentIdAndDeletedFalse(id)) {
            throw new BadRequestException("Cannot delete a department that still has active users");
        }
        if (allocationRepository.existsByDepartmentId(id)) {
            throw new BadRequestException("Cannot delete a department with allocation history");
        }
        if (inventoryRequestRepository.existsByDepartmentId(id)) {
            throw new BadRequestException("Cannot delete a department with existing inventory requests");
        }
        departmentRepository.delete(department);
    }

    private DepartmentDTO toDto(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setDepartmentName(department.getDepartmentName());
        dto.setDepartmentCode(department.getDepartmentCode());
        return dto;
    }
}
