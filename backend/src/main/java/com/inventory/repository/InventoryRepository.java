package com.inventory.repository;

import com.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByDeletedFalseOrderByCreatedAtDesc();
    Optional<Inventory> findByBarcodeAndDeletedFalse(String barcode);

    // Checked against ALL rows (not just non-deleted) since the DB unique constraint
    // on barcode applies regardless of soft-delete state.
    boolean existsByBarcode(String barcode);

    long countByDeletedFalse();
    long countByDeletedFalseAndAvailableQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAllocatedQuantityGreaterThan(int quantity);
    long countByDeletedFalseAndAvailableQuantityLessThanEqual(int quantity);

    // Lets a department browse what's currently available in a category before
    // deciding what (or whether) to request.
    List<Inventory> findByCategoryIdAndDeletedFalseOrderByCreatedAtDesc(UUID categoryId);

    // Used when deleting a category, to preserve inventory integrity.
    boolean existsByCategoryId(UUID categoryId);
}
