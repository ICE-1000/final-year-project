package com.inventory.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

// One row per REGISTRATION EVENT - distinct from Inventory (the brand/spec group) and
// InventoryUnit (one physical item). If the same group is restocked three times across
// the year, that's three InventoryBatch rows, each with its own date and quantity, even
// though all the units they produced belong to the same Inventory group. This is what
// lets you answer "which specific registration did this unit come from?" - not just
// "which group is it part of?" - see InventoryUnit.batch.
@Entity
@Table(name = "inventory_batches")
public class InventoryBatch {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    // e.g. "2026-ELEC-0002" - year, category code, sequence within that category.
    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "quantity_registered", nullable = false)
    private int quantityRegistered;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public int getQuantityRegistered() { return quantityRegistered; }
    public void setQuantityRegistered(int quantityRegistered) { this.quantityRegistered = quantityRegistered; }
    public User getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(User registeredBy) { this.registeredBy = registeredBy; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
