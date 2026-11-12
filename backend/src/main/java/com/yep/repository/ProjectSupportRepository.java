package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.project.ProjectSupport;

public interface ProjectSupportRepository extends JpaRepository<ProjectSupport, Long> {

    List<ProjectSupport> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectSupport> findByPartnerUserIdOrderByCreatedAtDesc(Long partnerId);

    boolean existsByProjectProjectIdAndPartnerUserIdAndSupportType(Long projectId, Long partnerId,
            com.yep.domain.enums.SupportType supportType);
}
