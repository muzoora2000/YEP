package com.yep.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.ActivityStatus;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long>,
        JpaSpecificationExecutor<LearningActivity> {

    Page<LearningActivity> findByStatus(ActivityStatus status, Pageable pageable);

    List<LearningActivity> findByStatus(ActivityStatus status);

    List<LearningActivity> findByFacilitatorUserId(Long facilitatorId);

    List<LearningActivity> findByPartnerUserId(Long partnerId);

    long countByFacilitatorUserId(Long facilitatorId);

    Page<LearningActivity> findByFacilitatorUserId(Long facilitatorId, Pageable pageable);

    Page<LearningActivity> findByStatusAndTitleContainingIgnoreCase(ActivityStatus status, String title,
            Pageable pageable);

    long countByStatus(ActivityStatus status);
}
