package com.inventory.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryDTO {
    private UUID id;

    // Optional on create: the system auto-assigns a unique barcode when this is left blank.
    @Pattern(regexp = "^$|^[A-Z0-9\\-]{3,100}$", message = "Barcode must be 3-100 characters: A-Z, 0-9, or hyphen")
    private String barcode;

    @NotBlank
    private String inventoryName;

    // Required: every item must belong to one of the categories an admin has created
    // (see CategoryController). categoryName is read-only, populated from the
    // relation for display - the client never sets it directly.
    @NotNull(message = "Category is required")
    private UUID categoryId;
    private String categoryName;
    private String categoryCode;

    private String description;
    @Min(0)
    private int quantity;
    private int availableQuantity;
    private int allocatedQuantity;
    private String serialNumber;
    private String condition;
    private String status;
    private String barcodeImageUrl;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getInventoryName() { return inventoryName; }
    public void setInventoryName(String inventoryName) { this.inventoryName = inventoryName; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public int getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(int allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBarcodeImageUrl() { return barcodeImageUrl; }
    public void setBarcodeImageUrl(String barcodeImageUrl) { this.barcodeImageUrl = barcodeImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
