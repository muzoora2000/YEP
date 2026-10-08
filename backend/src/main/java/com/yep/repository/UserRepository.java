package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.enums.Role;
import com.yep.domain.user.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByRoleAndAccountStatus(Role role, com.yep.domain.enums.AccountStatus status, Pageable pageable);

    long countByRole(Role role);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> search(@Param("q") String query, Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE role = :role AND LOWER(full_name) LIKE LOWER(CONCAT('%', :q, '%'))",
            countQuery = "SELECT COUNT(*) FROM users WHERE role = :role AND LOWER(full_name) LIKE LOWER(CONCAT('%', :q, '%'))",
            nativeQuery = true)
    Page<User> searchByRole(@Param("role") String role, @Param("q") String query, Pageable pageable);
}
