package com.inventory.repository;

import com.inventory.model.InventoryRequest;
import com.inventory.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, UUID> {
    // Join fetch department to avoid LazyInitializationException when converting to DTO
    @Query("SELECT r FROM InventoryRequest r JOIN FETCH r.department WHERE r.department.id = :departmentId ORDER BY r.createdAt DESC")
    List<InventoryRequest> findByDepartmentIdWithDepartment(@Param("departmentId") UUID departmentId);

    // Keep original for other uses if needed
    List<InventoryRequest> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);

    List<InventoryRequest> findAllByOrderByCreatedAtDesc();
    List<InventoryRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
}
