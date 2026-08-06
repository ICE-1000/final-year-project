package com.inventory.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    // Short unique identifier for this category (e.g. "ELEC" for Electronics).
    // This is the "category barcode" component: printable on its own via
    // GET /api/barcode/image/{code}, and used as the prefix for every inventory
    // item's barcode and every allocation's composite barcode.
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Next number to hand out for an inventory item auto-assigned to this category
    // (see InventoryService). Incremented atomically under a row lock so two
    // concurrent item creations in the same category never get the same number.
    @Column(name = "next_inventory_sequence", nullable = false)
    private long nextInventorySequence = 1L;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getNextInventorySequence() { return nextInventorySequence; }
    public void setNextInventorySequence(long nextInventorySequence) { this.nextInventorySequence = nextInventorySequence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
