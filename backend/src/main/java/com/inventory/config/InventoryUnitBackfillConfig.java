package com.inventory.config;

import com.inventory.model.Inventory;
import com.inventory.model.InventoryStatus;
import com.inventory.model.InventoryUnit;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.InventoryUnitRepository;
import com.inventory.service.BarcodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// One-time, idempotent backfill: any Inventory group created before individual-unit
// tracking was added has zero InventoryUnit rows, which would make it unscannable and
// unallocatable under the new unit-based system. This generates units retroactively for
// any such group - `availableQuantity` marked AVAILABLE, `allocatedQuantity` marked
// ALLOCATED (though pre-existing allocated ones can't be linked back to a specific
// historical Allocation row, since old allocations didn't track individual units - their
// allocation_id just stays null) - so every pre-existing item becomes scannable and
// allocatable exactly like anything registered from now on. Skips any group that
// already has units, so it's safe to run on every startup.
@Configuration
public class InventoryUnitBackfillConfig {
    private static final Logger log = LoggerFactory.getLogger(InventoryUnitBackfillConfig.class);

    @Bean
    public CommandLineRunner backfillInventoryUnits(InventoryRepository inventoryRepository,
                                                      InventoryUnitRepository unitRepository,
                                                      BarcodeService barcodeService) {
        return args -> runBackfill(inventoryRepository, unitRepository, barcodeService);
    }

    @Transactional
    void runBackfill(InventoryRepository inventoryRepository, InventoryUnitRepository unitRepository,
                      BarcodeService barcodeService) {
        List<Inventory> groups = inventoryRepository.findAll();
        int backfilledGroups = 0;
        int backfilledUnits = 0;
        for (Inventory group : groups) {
            if (unitRepository.existsByInventoryId(group.getId())) {
                continue;
            }
            int available = Math.max(group.getAvailableQuantity(), 0);
            int allocated = Math.max(group.getAllocatedQuantity(), 0);
            int total = Math.max(group.getQuantity(), available + allocated);
            if (total <= 0) {
                continue;
            }
            long seq = group.getNextUnitSequence() == null ? 1L : group.getNextUnitSequence();
            List<InventoryUnit> units = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                String unitBarcode = group.getBarcode() + "-" + String.format("%04d", seq);
                InventoryUnit unit = new InventoryUnit();
                unit.setInventory(group);
                unit.setUnitNumber((int) seq);
                unit.setUnitBarcode(unitBarcode);
                unit.setUnitBarcodeImageUrl(barcodeService.storeBarcodeImage(unitBarcode));
                unit.setStatus(i < available ? InventoryStatus.AVAILABLE : InventoryStatus.ALLOCATED);
                units.add(unit);
                seq++;
            }
            group.setNextUnitSequence(seq);
            inventoryRepository.save(group);
            unitRepository.saveAll(units);
            backfilledGroups++;
            backfilledUnits += units.size();
        }
        if (backfilledGroups > 0) {
            log.info("Backfilled {} individual inventory unit(s) across {} existing item group(s) "
                    + "that predated unit tracking.", backfilledUnits, backfilledGroups);
        }
    }
}
