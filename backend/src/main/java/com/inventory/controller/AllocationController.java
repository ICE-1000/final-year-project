package com.inventory.controller;

import com.inventory.dto.AllocationDTO;
import com.inventory.dto.AllocationRequest;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.service.AllocationService;
import com.inventory.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {
    private final AllocationService allocationService;
    private final UserService userService;

    public AllocationController(AllocationService allocationService, UserService userService) {
        this.allocationService = allocationService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AllocationDTO>> all() {
        return ResponseEntity.ok(allocationService.findAll());
    }

    // A DEPARTMENT user may only pass their own department's id; ADMIN may pass any.
    // (Previously this had no ownership check at all - any authenticated user could
    // read any other department's allocations just by changing the UUID in the URL.)
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<AllocationDTO>> byDepartment(@PathVariable UUID departmentId) {
        User currentUser = userService.getCurrentUser();
        assertCanAccessDepartment(currentUser, departmentId);
        return ResponseEntity.ok(allocationService.findByDepartment(departmentId));
    }

    // Convenience endpoint: a DEPARTMENT user doesn't need to know their own department's
    // UUID to see their own allocations.
    @PreAuthorize("hasRole('DEPARTMENT')")
    @GetMapping("/me")
    public ResponseEntity<List<AllocationDTO>> myAllocations() {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(allocationService.findByDepartment(currentUser.getDepartment().getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationDTO> allocate(@Valid @RequestBody AllocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationService.allocate(request));
    }

    private void assertCanAccessDepartment(User currentUser, UUID departmentId) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getDepartment() == null || !currentUser.getDepartment().getId().equals(departmentId)) {
            throw new AccessDeniedException("You can only access your own department's allocations");
        }
    }
}
