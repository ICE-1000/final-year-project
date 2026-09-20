package com.inventory.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public class DepartmentDTO {
    private UUID id;

    @NotBlank(message = "Department name is required")
    @Size(max = 150)
    private String departmentName;

    @NotBlank(message = "Department code is required")
    @Pattern(regexp = "^[A-Za-z0-9]{1,20}$", message = "Code must be 1-20 letters/numbers, no spaces")
    private String departmentCode;

    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
