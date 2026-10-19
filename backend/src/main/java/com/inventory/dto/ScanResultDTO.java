package com.inventory.dto;

public class ScanResultDTO {
    private String type;
    private InventoryUnitDTO unit;

    public ScanResultDTO() {}

    public ScanResultDTO(String type, InventoryUnitDTO unit) {
        this.type = type;
        this.unit = unit;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public InventoryUnitDTO getUnit() { return unit; }
    public void setUnit(InventoryUnitDTO unit) { this.unit = unit; }
}
