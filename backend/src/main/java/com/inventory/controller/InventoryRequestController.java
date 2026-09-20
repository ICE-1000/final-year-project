package com.inventory.controller;

import com.inventory.dto.CreateRequestDTO;
import com.inventory.dto.InventoryRequestDTO;
import com.inventory.dto.UpdateRequestStatusDTO;
import com.inventory.model.User;
import com.inventory.service.InventoryRequestService;
import com.inventory.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class InventoryRequestController {
    private final InventoryRequestService requestService;
    private final UserService userService;

    public InventoryRequestController(InventoryRequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Requests controller is reachable");
    }

    @PreAuthorize("hasRole('DEPARTMENT')")
    @PostMapping
    public ResponseEntity<InventoryRequestDTO> createRequest(@Valid @RequestBody CreateRequestDTO dto) {
        User currentUser = userService.getCurrentUser();
        InventoryRequestDTO created = requestService.createRequest(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('DEPARTMENT')")
    @GetMapping("/me")
    public ResponseEntity<List<InventoryRequestDTO>> myRequests() {
        User currentUser = userService.getCurrentUser();
        UUID departmentId = currentUser.getDepartment() == null ? null : currentUser.getDepartment().getId();
        if (departmentId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(requestService.getRequestsForDepartment(departmentId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<InventoryRequestDTO>> allRequests(@RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(requestService.getAllRequests(status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<InventoryRequestDTO> updateStatus(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateRequestStatusDTO dto) {
        User admin = userService.getCurrentUser();
        return ResponseEntity.ok(requestService.updateRequestStatus(id, dto, admin));
    }
}
