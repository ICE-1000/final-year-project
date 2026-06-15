// com/inventory/repository/DepartmentRegistrationRepository.java
package com.inventory.repository;

import com.inventory.model.DepartmentRegistration;
import com.inventory.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DepartmentRegistrationRepository extends JpaRepository<DepartmentRegistration, UUID> {
    List<DepartmentRegistration> findByStatusOrderByCreatedAtAsc(RegistrationStatus status);
    boolean existsByDepartmentCode(String departmentCode);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}