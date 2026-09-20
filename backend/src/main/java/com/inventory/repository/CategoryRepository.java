package com.inventory.repository;

import com.inventory.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    List<Category> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") UUID id);
}
