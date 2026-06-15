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
        if (registrationRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new BadRequestException("Department code already registered (pending or approved)");
        }
        if (departmentRepository.findByDepartmentCode(request.getDepartmentCode()).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }
        if (registrationRepository.existsByUsername(request.getUsername()) ||
            userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (registrationRepository.existsByEmail(request.getEmail()) ||
            userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BadRequestException("Email already used");
        }

        DepartmentRegistration reg = new DepartmentRegistration();
        reg.setDepartmentName(request.getDepartmentName());
        reg.setDepartmentCode(request.getDepartmentCode().toUpperCase());
        reg.setUsername(request.getUsername());
        reg.setEmail(request.getEmail());
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
        if (departmentRepository.findByDepartmentCode(reg.getDepartmentCode()).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }

        Department department = new Department();
        department.setDepartmentName(reg.getDepartmentName());
        department.setDepartmentCode(reg.getDepartmentCode());
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