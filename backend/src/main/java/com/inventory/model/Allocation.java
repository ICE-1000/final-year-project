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

@Entity
@Table(name = "allocations")
public class Allocation {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by")
    private User allocatedBy;

    private LocalDateTime allocatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private AllocationStatus status = AllocationStatus.PENDING;

    // Composite, printable barcode identifying this specific allocation:
    // {year}-{categoryCode}-{inventoryBarcode}-{departmentCode}-{uniqueSuffix}.
    // The trailing suffix exists purely to guarantee uniqueness if the same item
    // is ever allocated to the same department more than once in the same year -
    // without it, those two allocations would produce an identical code and
    // scanning either one would be ambiguous about which allocation it means.
    @Column(name = "allocation_barcode", unique = true, length = 150)
    private String allocationBarcode;

    @Column(name = "allocation_barcode_image_url", columnDefinition = "TEXT")
    private String allocationBarcodeImageUrl;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public User getAllocatedBy() { return allocatedBy; }
    public void setAllocatedBy(User allocatedBy) { this.allocatedBy = allocatedBy; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
    public AllocationStatus getStatus() { return status; }
    public void setStatus(AllocationStatus status) { this.status = status; }
    public String getAllocationBarcode() { return allocationBarcode; }
    public void setAllocationBarcode(String allocationBarcode) { this.allocationBarcode = allocationBarcode; }
    public String getAllocationBarcodeImageUrl() { return allocationBarcodeImageUrl; }
    public void setAllocationBarcodeImageUrl(String allocationBarcodeImageUrl) { this.allocationBarcodeImageUrl = allocationBarcodeImageUrl; }
}
