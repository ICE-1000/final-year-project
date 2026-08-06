package com.inventory.dto;

// Response shape for the unified GET /api/barcode/scan/{barcode} endpoint.
// `type` tells the caller which of the two fields is populated:
//   - "INVENTORY": an item's own barcode was scanned -> `inventory` is set
//   - "ALLOCATION": a composite allocation barcode was scanned -> `allocation` is set
public class ScanResultDTO {
    private String type;
    private InventoryDTO inventory;
    private AllocationDTO allocation;

    public ScanResultDTO() {}

    public ScanResultDTO(String type, InventoryDTO inventory, AllocationDTO allocation) {
        this.type = type;
        this.inventory = inventory;
        this.allocation = allocation;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public InventoryDTO getInventory() { return inventory; }
    public void setInventory(InventoryDTO inventory) { this.inventory = inventory; }
    public AllocationDTO getAllocation() { return allocation; }
    public void setAllocation(AllocationDTO allocation) { this.allocation = allocation; }
}
