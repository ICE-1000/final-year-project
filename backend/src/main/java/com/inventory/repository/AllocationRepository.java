package com.inventory.repository;

import com.inventory.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllocationRepository extends JpaRepository<Allocation, UUID> {
    List<Allocation> findByDepartmentIdOrderByAllocatedAtDesc(UUID departmentId);
    List<Allocation> findAllByOrderByAllocatedAtDesc();
}
