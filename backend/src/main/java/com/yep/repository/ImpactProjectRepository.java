package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.project.ImpactProject;

public interface ImpactProjectRepository extends JpaRepository<ImpactProject, Long> {

    Optional<ImpactProject> findByTeamTeamId(Long teamId);

    List<ImpactProject> findByStatus(ProjectStatus status);

    List<ImpactProject> findByStatusAndOpenForSupportTrue(ProjectStatus status);

    List<ImpactProject> findByOpenForSupportTrueAndStatusIn(List<ProjectStatus> statuses);

    List<ImpactProject> findByTeamMentorUserId(Long mentorId);

    long countByStatus(ProjectStatus status);
}
