package com.inventory.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryDTO {
    private UUID id;

    @Pattern(regexp = "^$|^[A-Z0-9\\-]{3,100}$", message = "Barcode must be 3-100 characters: A-Z, 0-9, or hyphen")
    private String barcode;

    @NotBlank
    private String inventoryName;

    @NotNull(message = "Category is required")
    private UUID categoryId;
    private String categoryName;
    private String categoryCode;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String specification;

    private String description;

    // On create: how many units to register now (0 is valid - defines an empty
    // placeholder group). On an existing-group match, this is how many to add.
    // On update: ignored entirely - quantity can only change via re-registration.
    @Min(0)
    private int quantity;

    private Integer availableQuantity;
    private Integer allocatedQuantity;

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
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }
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
