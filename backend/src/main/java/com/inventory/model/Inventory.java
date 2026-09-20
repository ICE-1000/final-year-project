package com.inventory.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

// Represents a *group* of identical (or near-identical) items - same category, name,
// brand and specification - not a single physical item. Individual physical items
// within this group are tracked separately as InventoryUnit rows, each with its own
// permanent barcode. quantity/availableQuantity/allocatedQuantity are aggregate counts
// maintained by InventoryService alongside unit creation/allocation, kept here mainly
// so list/stats views don't need a COUNT(*) over units on every request.
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    // The group's base/prefix code (e.g. "ELEC-0007") - individual units get
    // "{this}-{4-digit unit number}" as their own permanent barcode.
    @Column(nullable = false, unique = true, length = 100)
    private String barcode;

    @Column(name = "inventory_name", nullable = false)
    private String inventoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 150)
    private String brand;

    @Column(length = 150)
    private String specification;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "allocated_quantity", nullable = false)
    private int allocatedQuantity;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "condition")
    private String condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Column(name = "barcode_image_url", columnDefinition = "TEXT")
    private String barcodeImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean deleted = false;

    // Next unit number to hand out for THIS group's individual units. Boxed/nullable
    // (see class comment) - null is treated as "1" (unstarted) anywhere it's read.
    @Column(name = "next_unit_sequence")
    private Long nextUnitSequence = 1L;

    @Version
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getInventoryName() { return inventoryName; }
    public void setInventoryName(String inventoryName) { this.inventoryName = inventoryName; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
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
    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }
    public String getBarcodeImageUrl() { return barcodeImageUrl; }
    public void setBarcodeImageUrl(String barcodeImageUrl) { this.barcodeImageUrl = barcodeImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getNextUnitSequence() { return nextUnitSequence; }
    public void setNextUnitSequence(Long nextUnitSequence) { this.nextUnitSequence = nextUnitSequence; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
