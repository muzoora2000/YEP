package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.learning.LearningMaterial;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {

    List<LearningMaterial> findByActivityActivityIdAndPublishedTrueOrderByUploadedAtAsc(Long activityId);

    List<LearningMaterial> findByActivityActivityIdOrderByUploadedAtAsc(Long activityId);
}
