package com.inventory.repository;

import com.inventory.model.InventoryRequest;
import com.inventory.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, UUID> {
    // Join fetch department AND category to avoid LazyInitializationException / N+1
    // when converting to DTO.
    @Query("SELECT r FROM InventoryRequest r JOIN FETCH r.department LEFT JOIN FETCH r.category WHERE r.department.id = :departmentId ORDER BY r.createdAt DESC")
    List<InventoryRequest> findByDepartmentIdWithDepartment(@Param("departmentId") UUID departmentId);

    @Query("SELECT r FROM InventoryRequest r JOIN FETCH r.department LEFT JOIN FETCH r.category ORDER BY r.createdAt DESC")
    List<InventoryRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM InventoryRequest r JOIN FETCH r.department LEFT JOIN FETCH r.category WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<InventoryRequest> findByStatusOrderByCreatedAtDesc(@Param("status") RequestStatus status);

    boolean existsByDepartmentId(UUID departmentId);
    boolean existsByCategoryId(UUID categoryId);
}
