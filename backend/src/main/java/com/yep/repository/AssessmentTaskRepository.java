package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.learning.AssessmentTask;

public interface AssessmentTaskRepository extends JpaRepository<AssessmentTask, Long> {

    List<AssessmentTask> findByActivityActivityIdAndPublishedTrueOrderByDueDateAsc(Long activityId);

    List<AssessmentTask> findByActivityActivityIdOrderByDueDateAsc(Long activityId);
}
