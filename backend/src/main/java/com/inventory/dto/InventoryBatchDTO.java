package com.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryBatchDTO {
    private UUID id;
    private String batchCode;
    private UUID inventoryId;
    private String inventoryName;
    private int quantityRegistered;
    private String registeredByUsername;
    private LocalDateTime registeredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public UUID getInventoryId() { return inventoryId; }
    public void setInventoryId(UUID inventoryId) { this.inventoryId = inventoryId; }
    public String getInventoryName() { return inventoryName; }
    public void setInventoryName(String inventoryName) { this.inventoryName = inventoryName; }
    public int getQuantityRegistered() { return quantityRegistered; }
    public void setQuantityRegistered(int quantityRegistered) { this.quantityRegistered = quantityRegistered; }
    public String getRegisteredByUsername() { return registeredByUsername; }
    public void setRegisteredByUsername(String registeredByUsername) { this.registeredByUsername = registeredByUsername; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
