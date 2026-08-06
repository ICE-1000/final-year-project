package com.inventory.repository;

import com.inventory.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllocationRepository extends JpaRepository<Allocation, UUID> {
    List<Allocation> findByDepartmentIdOrderByAllocatedAtDesc(UUID departmentId);
    List<Allocation> findAllByOrderByAllocatedAtDesc();

    // Used when deleting a department, to preserve allocation history integrity.
    boolean existsByDepartmentId(UUID departmentId);

    // Used by the unified barcode scan endpoint to resolve a scanned allocation
    // barcode back to the allocation it identifies.
    Optional<Allocation> findByAllocationBarcode(String allocationBarcode);
}
