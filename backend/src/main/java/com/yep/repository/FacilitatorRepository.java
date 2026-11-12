package com.yep.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.Facilitator;

public interface FacilitatorRepository extends JpaRepository<Facilitator, Long> {
}
