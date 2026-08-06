package com.inventory.dto;

import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class CreateRequestDTO {
    // Required: the department picks a category first (and can see what's currently
    // available in it via GET /api/inventory/category/{categoryId}) before submitting.
    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank
    private String itemName;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @FutureOrPresent(message = "Needed-by date cannot be in the past")
    private LocalDate neededBy;

    private String description;

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDate getNeededBy() { return neededBy; }
    public void setNeededBy(LocalDate neededBy) { this.neededBy = neededBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
