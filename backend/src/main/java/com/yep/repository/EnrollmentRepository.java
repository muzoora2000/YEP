package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.learning.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByYouthUserId(Long youthId);

    List<Enrollment> findByActivityActivityId(Long activityId);

    List<Enrollment> findByActivityActivityIdAndStatus(Long activityId, EnrollmentStatus status);

    Optional<Enrollment> findByYouthUserIdAndActivityActivityId(Long youthId, Long activityId);

    boolean existsByYouthUserIdAndActivityActivityId(Long youthId, Long activityId);

    long countByActivityActivityIdAndStatus(Long activityId, EnrollmentStatus status);

    Optional<Enrollment> findFirstByActivityActivityIdAndStatusOrderByEnrollmentDateAsc(Long activityId, EnrollmentStatus status);

    long countByActivityActivityIdAndStatusIn(Long activityId, List<EnrollmentStatus> statuses);
}
