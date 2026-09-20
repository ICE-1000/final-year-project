package com.inventory.repository;

import com.inventory.model.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {
    @Query("SELECT b FROM InventoryBatch b JOIN FETCH b.inventory LEFT JOIN FETCH b.registeredBy " +
           "WHERE b.inventory.id = :inventoryId ORDER BY b.registeredAt DESC")
    List<InventoryBatch> findByInventoryIdWithDetails(@Param("inventoryId") UUID inventoryId);
}
