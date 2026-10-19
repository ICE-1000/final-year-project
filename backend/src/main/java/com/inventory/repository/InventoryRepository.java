package com.inventory.repository;

import com.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByDeletedFalseOrderByCreatedAtDesc();
    Optional<Inventory> findByBarcodeAndDeletedFalse(String barcode);

    boolean existsByBarcode(String barcode);

    long countByDeletedFalse();
    long countByDeletedFalseAndAvailableQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAllocatedQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAvailableQuantityLessThanEqual(int quantity);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.category WHERE i.category.id = :categoryId AND i.deleted = false ORDER BY i.createdAt DESC")
    List<Inventory> findByCategoryIdAndDeletedFalseOrderByCreatedAtDesc(@Param("categoryId") UUID categoryId);

    boolean existsByCategoryId(UUID categoryId);

    @Query("SELECT i FROM Inventory i WHERE i.category.id = :categoryId AND i.deleted = false " +
           "AND LOWER(i.inventoryName) = LOWER(:name) " +
           "AND COALESCE(LOWER(i.brand), '') = COALESCE(LOWER(:brand), '') " +
           "AND COALESCE(LOWER(i.specification), '') = COALESCE(LOWER(:specification), '')")
    Optional<Inventory> findMatchingGroup(@Param("categoryId") UUID categoryId, @Param("name") String name,
                                           @Param("brand") String brand, @Param("specification") String specification);
}
