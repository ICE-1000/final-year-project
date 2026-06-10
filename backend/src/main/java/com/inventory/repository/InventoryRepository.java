package com.inventory.repository;

import com.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByDeletedFalseOrderByCreatedAtDesc();
    Optional<Inventory> findByBarcodeAndDeletedFalse(String barcode);
    long countByDeletedFalse();
    long countByDeletedFalseAndAvailableQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAllocatedQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAvailableQuantityLessThanEqual(int quantity);
}
