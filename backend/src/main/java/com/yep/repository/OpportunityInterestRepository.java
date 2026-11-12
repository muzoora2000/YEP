package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.partner.OpportunityInterest;

public interface OpportunityInterestRepository extends JpaRepository<OpportunityInterest, Long> {

    List<OpportunityInterest> findByOpportunityOpportunityIdOrderByCreatedAtDesc(Long opportunityId);

    List<OpportunityInterest> findByYouthUserId(Long youthId);

    boolean existsByOpportunityOpportunityIdAndYouthUserId(Long opportunityId, Long youthId);

    Optional<OpportunityInterest> findByOpportunityOpportunityIdAndYouthUserId(Long opportunityId, Long youthId);

    long countByOpportunityOpportunityId(Long opportunityId);
}
