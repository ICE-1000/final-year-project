package com.inventory.model;

// Mirrors the values previously stored as free-form strings on Inventory.status,
// now enforced at compile time so a typo can no longer silently corrupt filtering/reporting.
public enum InventoryStatus {
    AVAILABLE,
    ALLOCATED
}
