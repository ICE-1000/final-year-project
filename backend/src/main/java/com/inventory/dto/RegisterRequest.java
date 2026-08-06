package com.inventory.dto;

import com.inventory.model.Role;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

// NOTE: this DTO carries an explicit role, which is why /api/auth/register
// is now restricted to callers already authenticated as ADMIN (see SecurityConfig).
// It must never be reachable by an unauthenticated caller.
public class RegisterRequest {
    @NotBlank
    private String username;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;
    @NotNull
    private Role role;
    private UUID departmentId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
