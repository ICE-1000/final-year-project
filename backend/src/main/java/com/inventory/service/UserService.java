package com.inventory.service;

import com.inventory.dto.RegisterRequest;
import com.inventory.dto.UserDTO;
import com.inventory.dto.UserUpdateRequest;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Department;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.DepartmentRepository;
import com.inventory.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (request.getRole() == Role.DEPARTMENT && request.getDepartmentId() == null) {
            throw new BadRequestException("Department ID is required for DEPARTMENT role");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        }
        return userRepository.save(user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("No authenticated user");
        }
        return userRepository.findByUsernameWithDepartment(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAllUsers() {
        return userRepository.findByDeletedFalse().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (userRepository.existsByUsernameAndIdNotAndDeletedFalse(request.getUsername(), id)) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmailAndIdNotAndDeletedFalse(request.getEmail(), id)) {
            throw new BadRequestException("Email already exists");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(request.getRole());
        if (request.getRole() == Role.DEPARTMENT) {
            if (request.getDepartmentId() == null) {
                throw new BadRequestException("Department is required for department users");
            }
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    public List<UserDTO> findAll() {
        return findAllUsers();
    }
    

    public UserDTO update(UUID id, UserUpdateRequest request) {
        return updateUser(id, request);
    }

    public void delete(UUID id) {
        deleteUser(id);
    }

    private UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus("ACTIVE");
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getDepartmentName());
        }
        return dto;
    }
}
