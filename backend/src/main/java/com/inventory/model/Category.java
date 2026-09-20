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

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "next_inventory_sequence", nullable = false)
    private long nextInventorySequence = 1L;

    // Next batch number for this category, used in batch codes like "2026-ELEC-0002"
    // (year-categoryCode-sequence). Boxed/nullable so ALTER TABLE ADD COLUMN succeeds
    // on an already-populated categories table without needing a DEFAULT; treated as
    // "1" wherever it's read if null. Increments on every registration event in this
    // category (new group or adding to an existing one) - see InventoryService.create.
    @Column(name = "next_batch_sequence")
    private Long nextBatchSequence = 1L;

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
    public Long getNextBatchSequence() { return nextBatchSequence; }
    public void setNextBatchSequence(Long nextBatchSequence) { this.nextBatchSequence = nextBatchSequence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
