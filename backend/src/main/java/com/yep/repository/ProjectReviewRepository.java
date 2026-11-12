package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.project.ProjectReview;

public interface ProjectReviewRepository extends JpaRepository<ProjectReview, Long> {

    List<ProjectReview> findByProjectProjectIdOrderByCreatedAtAsc(Long projectId);

    List<ProjectReview> findByMentorUserId(Long mentorId);

    long countByMentorUserId(Long mentorId);
}
