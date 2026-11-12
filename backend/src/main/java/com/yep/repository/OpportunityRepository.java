package com.yep.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.OpportunityStatus;
import com.yep.domain.partner.Opportunity;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Page<Opportunity> findByStatus(OpportunityStatus status, Pageable pageable);

    List<Opportunity> findByPartnerUserIdOrderByCreatedAtDesc(Long partnerId);

    long countByStatus(OpportunityStatus status);
}
