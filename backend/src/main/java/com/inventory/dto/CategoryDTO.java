package com.inventory.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryDTO {
    private UUID id;

    @NotBlank
    @Size(max = 100)
    private String name;

    // Short unique code, e.g. "ELEC". Normalized to uppercase server-side.
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "Code must be 2-10 letters/numbers, no spaces")
    private String code;

    @Size(max = 500)
    private String description;

    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
