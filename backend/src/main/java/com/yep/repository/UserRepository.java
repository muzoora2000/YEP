package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.user.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    List<User> findByStatus(AccountStatus status);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    long countByRole(Role role);

    long countByStatus(AccountStatus status);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
              and (:query is null or lower(u.name) like lower(concat('%', :query, '%'))
                   or lower(u.email) like lower(concat('%', :query, '%')))
            """)
    Page<User> search(@Param("role") Role role, @Param("status") AccountStatus status,
            @Param("query") String query, Pageable pageable);
}
