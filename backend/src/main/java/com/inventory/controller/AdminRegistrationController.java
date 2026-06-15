// com/inventory/controller/AdminRegistrationController.java
package com.inventory.controller;

import com.inventory.dto.RegistrationResponseDTO;
import com.inventory.service.DepartmentRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/registrations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistrationController {
    private final DepartmentRegistrationService registrationService;

    public AdminRegistrationController(DepartmentRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public ResponseEntity<List<RegistrationResponseDTO>> getPending() {
        return ResponseEntity.ok(registrationService.getPendingRegistrations());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id) {
        registrationService.approveRegistration(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id, @RequestParam String reason) {
        registrationService.rejectRegistration(id, reason);
        return ResponseEntity.ok().build();
    }
}