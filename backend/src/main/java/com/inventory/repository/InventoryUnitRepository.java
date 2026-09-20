package com.inventory.repository;

import com.inventory.model.InventoryUnit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryUnitRepository extends JpaRepository<InventoryUnit, UUID> {

    boolean existsByInventoryId(UUID inventoryId);

    @Query("SELECT u FROM InventoryUnit u JOIN FETCH u.inventory i LEFT JOIN FETCH i.category " +
           "LEFT JOIN FETCH u.allocation a LEFT JOIN FETCH a.department LEFT JOIN FETCH u.batch " +
           "WHERE u.unitBarcode = :barcode")
    Optional<InventoryUnit> findByUnitBarcodeWithDetails(@Param("barcode") String barcode);

    @Query("SELECT u FROM InventoryUnit u JOIN FETCH u.inventory i LEFT JOIN FETCH i.category " +
           "LEFT JOIN FETCH u.allocation a LEFT JOIN FETCH a.department LEFT JOIN FETCH u.batch " +
           "WHERE a.id = :allocationId ORDER BY u.unitNumber ASC")
    List<InventoryUnit> findByAllocationIdWithDetails(@Param("allocationId") UUID allocationId);

    @Query("SELECT u FROM InventoryUnit u JOIN FETCH u.inventory i LEFT JOIN FETCH i.category " +
           "LEFT JOIN FETCH u.allocation a LEFT JOIN FETCH a.department LEFT JOIN FETCH u.batch " +
           "WHERE i.id = :inventoryId ORDER BY u.unitNumber ASC")
    List<InventoryUnit> findByInventoryIdWithDetailsOrderByUnitNumberAsc(@Param("inventoryId") UUID inventoryId);

    // Row-locked selection of the next N available units for a group, used by
    // AllocationService.allocate() so two concurrent allocation requests against the
    // same group can never both grab the same physical unit - each row this returns is
    // locked (SELECT ... FOR UPDATE) until the enclosing transaction commits.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM InventoryUnit u WHERE u.inventory.id = :inventoryId AND u.status = 'AVAILABLE' " +
           "ORDER BY u.unitNumber ASC")
    List<InventoryUnit> findAvailableUnitsForUpdate(@Param("inventoryId") UUID inventoryId, Pageable pageable);
}
