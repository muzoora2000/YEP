package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.Mentor;

public interface MentorRepository extends JpaRepository<Mentor, Long> {

    List<Mentor> findByAccountStatus(com.yep.domain.enums.AccountStatus status);
}
