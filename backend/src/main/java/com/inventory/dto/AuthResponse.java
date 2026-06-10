package com.inventory.dto;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private String role;
    private UUID userId;
    private UUID departmentId;

    public AuthResponse(String token, String role, UUID userId, UUID departmentId) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.departmentId = departmentId;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
    public UUID getUserId() { return userId; }
    public UUID getDepartmentId() { return departmentId; }
}
