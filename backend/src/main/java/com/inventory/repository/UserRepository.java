package com.inventory.repository;

import com.inventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Fetch only active (not deleted) users
    List<User> findByDeletedFalse();

    // Authentication – exclude deleted users
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deleted = false")
    Optional<User> findByUsername(@Param("username") String username);

    // For UserDetailsService – also exclude deleted
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.username = :username AND u.deleted = false")
    Optional<User> findByUsernameWithDepartment(@Param("username") String username);

    // For current user lookup
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithDepartment(@Param("id") UUID id);

    // Existence checks ignoring deleted users
    boolean existsByUsernameAndDeletedFalse(String username);
    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByUsernameAndIdNotAndDeletedFalse(String username, UUID id);
    boolean existsByEmailAndIdNotAndDeletedFalse(String email, UUID id);
}
