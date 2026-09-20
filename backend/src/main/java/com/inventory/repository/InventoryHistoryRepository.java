package com.inventory.repository;

import com.inventory.model.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, UUID> {
}
