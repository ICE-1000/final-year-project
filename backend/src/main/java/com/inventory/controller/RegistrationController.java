
package com.inventory.controller;

import com.inventory.dto.RegisterDepartmentRequest;
import com.inventory.service.DepartmentRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final DepartmentRegistrationService registrationService;

    public RegistrationController(DepartmentRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/department/register")
    public ResponseEntity<Void> registerDepartment(@Valid @RequestBody RegisterDepartmentRequest request) {
        registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}