package com.inventory.dto;

import java.util.UUID;

public class DepartmentDTO {
    private UUID id;
    private String departmentName;
    private String departmentCode;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
}
