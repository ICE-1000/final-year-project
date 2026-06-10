package com.inventory.dto;

public class InventoryStatsDTO {
    private long total;
    private long available;
    private long allocated;
    private long lowStock;

    public InventoryStatsDTO(long total, long available, long allocated, long lowStock) {
        this.total = total;
        this.available = available;
        this.allocated = allocated;
        this.lowStock = lowStock;
    }

    public long getTotal() { return total; }
    public long getAvailable() { return available; }
    public long getAllocated() { return allocated; }
    public long getLowStock() { return lowStock; }
}
