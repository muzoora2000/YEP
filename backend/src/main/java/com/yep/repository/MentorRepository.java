package com.yep.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.Mentor;

public interface MentorRepository extends JpaRepository<Mentor, Long> {
}
