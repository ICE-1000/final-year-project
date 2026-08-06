package com.inventory.model;

// Mirrors the values previously stored as free-form strings on Allocation.status.
// CANCELLED is included for future use even though nothing sets it yet.
public enum AllocationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
