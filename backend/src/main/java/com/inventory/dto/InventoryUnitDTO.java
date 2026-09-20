package com.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// Full picture of one physical, individually-barcoded item: which group it belongs to
// (name/category/brand/specification/description/condition), its own permanent unit
// barcode, and - if currently allocated - which department and when. Returned by the
// barcode scan endpoint, the per-inventory-group unit list, and the per-allocation
// unit list, so all three views share one consistent shape.
public class InventoryUnitDTO {
    private UUID id;
    private UUID inventoryId;
    private String inventoryName;
    private UUID categoryId;
    private String categoryName;
    private String categoryCode;
    private String brand;
    private String specification;
    private String description;
    private String condition;
    private int unitNumber;
    private String unitBarcode;
    private String unitBarcodeImageUrl;
    private String status;
    private String batchCode;
    private UUID allocationId;
    private UUID departmentId;
    private String departmentName;
    private LocalDateTime allocatedAt;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInventoryId() { return inventoryId; }
    public void setInventoryId(UUID inventoryId) { this.inventoryId = inventoryId; }
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
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public int getUnitNumber() { return unitNumber; }
    public void setUnitNumber(int unitNumber) { this.unitNumber = unitNumber; }
    public String getUnitBarcode() { return unitBarcode; }
    public void setUnitBarcode(String unitBarcode) { this.unitBarcode = unitBarcode; }
    public String getUnitBarcodeImageUrl() { return unitBarcodeImageUrl; }
    public void setUnitBarcodeImageUrl(String unitBarcodeImageUrl) { this.unitBarcodeImageUrl = unitBarcodeImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public UUID getAllocationId() { return allocationId; }
    public void setAllocationId(UUID allocationId) { this.allocationId = allocationId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
