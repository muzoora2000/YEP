package com.yep.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.learning.Assessment;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByYouthUserId(Long youthId);

    List<Assessment> findByActivityActivityId(Long activityId);

    List<Assessment> findByTaskTaskId(Long taskId);

    Optional<Assessment> findByTaskTaskIdAndYouthUserId(Long taskId, Long youthId);

    boolean existsByTaskTaskIdAndYouthUserId(Long taskId, Long youthId);

    long countByActivityActivityIdAndStatus(Long activityId, com.yep.domain.enums.AssessmentStatus status);

    List<Assessment> findByYouthUserIdAndActivityActivityId(Long youthId, Long activityId);

    @Query("select max(a.assessmentDate) from Assessment a where a.youth.userId = :youthId and a.activity.activityId = :activityId")
    Optional<OffsetDateTime> findLastSubmissionDate(@Param("youthId") Long youthId, @Param("activityId") Long activityId);
}
