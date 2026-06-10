package com.inventory.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateRequestDTO {
    @NotBlank
    private String itemName;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private LocalDate neededBy;

    private String description;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDate getNeededBy() { return neededBy; }
    public void setNeededBy(LocalDate neededBy) { this.neededBy = neededBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
