// com/inventory/service/DepartmentRegistrationService.java
package com.inventory.service;

import com.inventory.dto.RegisterDepartmentRequest;
import com.inventory.dto.RegistrationResponseDTO;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.*;
import com.inventory.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DepartmentRegistrationService {
    private final DepartmentRegistrationRepository registrationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DepartmentRegistrationService(DepartmentRegistrationRepository registrationRepository,
                                         DepartmentRepository departmentRepository,
                                         UserRepository userRepository,
                                         PasswordEncoder passwordEncoder) {
        this.registrationRepository = registrationRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterDepartmentRequest request) {
        // Normalize inputs to avoid case-sensitivity issues in uniqueness checks
        final String deptCode = request.getDepartmentCode() == null ? null : request.getDepartmentCode().trim().toUpperCase();
        final String username = request.getUsername() == null ? null : request.getUsername().trim();
        final String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();

        if (deptCode == null || deptCode.isEmpty()) {
            throw new BadRequestException("Department code is required");
        }

        if (registrationRepository.existsByDepartmentCode(deptCode)) {
            throw new BadRequestException("Department code already registered (pending or approved)");
        }
        if (departmentRepository.findByDepartmentCode(deptCode).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }
        if (registrationRepository.existsByUsername(username) ||
            userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (registrationRepository.existsByEmail(email) ||
            userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new BadRequestException("Email already used");
        }

        DepartmentRegistration reg = new DepartmentRegistration();
        reg.setDepartmentName(request.getDepartmentName());
        reg.setDepartmentCode(deptCode);
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(passwordEncoder.encode(request.getPassword()));
        reg.setStatus(RegistrationStatus.PENDING);
        registrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponseDTO> getPendingRegistrations() {
        return registrationRepository.findByStatusOrderByCreatedAtAsc(RegistrationStatus.PENDING)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void approveRegistration(UUID registrationId) {
        DepartmentRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Registration already processed");
        }
        final String regDeptCode = reg.getDepartmentCode() == null ? null : reg.getDepartmentCode().trim().toUpperCase();
        if (departmentRepository.findByDepartmentCode(regDeptCode).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }

        Department department = new Department();
        department.setDepartmentName(reg.getDepartmentName());
        department.setDepartmentCode(regDeptCode);
        departmentRepository.save(department);

        User user = new User();
        user.setUsername(reg.getUsername());
        user.setEmail(reg.getEmail());
        user.setPassword(reg.getPassword());
        user.setRole(Role.DEPARTMENT);
        user.setDepartment(department);
        user.setDeleted(false);
        userRepository.save(user);

        reg.setStatus(RegistrationStatus.APPROVED);
        registrationRepository.save(reg);
    }

    @Transactional
    public void rejectRegistration(UUID registrationId, String reason) {
        DepartmentRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Registration already processed");
        }
        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectionReason(reason);
        registrationRepository.save(reg);
    }

    private RegistrationResponseDTO toDto(DepartmentRegistration reg) {
        RegistrationResponseDTO dto = new RegistrationResponseDTO();
        dto.setId(reg.getId());
        dto.setDepartmentName(reg.getDepartmentName());
        dto.setDepartmentCode(reg.getDepartmentCode());
        dto.setUsername(reg.getUsername());
        dto.setEmail(reg.getEmail());
        dto.setStatus(reg.getStatus().name());
        dto.setCreatedAt(reg.getCreatedAt());
        return dto;
    }
}