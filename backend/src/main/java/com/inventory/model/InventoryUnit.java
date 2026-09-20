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
import java.time.LocalDateTime;
import java.util.UUID;

// One row per physically-trackable item within an Inventory group (e.g. one specific
// pen out of a box of 50). Each unit gets its own permanent, unique barcode assigned
// once at creation - the same barcode is used for its whole lifecycle regardless of
// which department it's later allocated to, matching how a real asset tag works.
// Scanning a unit's barcode always resolves back to this row (and its parent group),
// so full specifications/brand/status are always recoverable from the physical label.
@Entity
@Table(name = "inventory_units")
public class InventoryUnit {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // Sequence within the parent group - continues across re-registrations (adding
    // more stock never restarts the numbering), so it's always a reliable, gapless
    // record of "the Nth unit of this item ever registered."
    @Column(name = "unit_number", nullable = false)
    private int unitNumber;

    @Column(name = "unit_barcode", nullable = false, unique = true, length = 150)
    private String unitBarcode;

    @Column(name = "unit_barcode_image_url", columnDefinition = "TEXT")
    private String unitBarcodeImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    // Which registration event produced this unit - null for units backfilled from
    // stock that existed before batch tracking was added (see
    // InventoryUnitBackfillConfig), always set for anything registered from now on.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public int getUnitNumber() { return unitNumber; }
    public void setUnitNumber(int unitNumber) { this.unitNumber = unitNumber; }
    public String getUnitBarcode() { return unitBarcode; }
    public void setUnitBarcode(String unitBarcode) { this.unitBarcode = unitBarcode; }
    public String getUnitBarcodeImageUrl() { return unitBarcodeImageUrl; }
    public void setUnitBarcodeImageUrl(String unitBarcodeImageUrl) { this.unitBarcodeImageUrl = unitBarcodeImageUrl; }
    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }
    public Allocation getAllocation() { return allocation; }
    public void setAllocation(Allocation allocation) { this.allocation = allocation; }
    public InventoryBatch getBatch() { return batch; }
    public void setBatch(InventoryBatch batch) { this.batch = batch; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
