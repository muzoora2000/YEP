package com.yep.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.project.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByProjectProjectId(Long projectId);

    List<Milestone> findByStatusNotAndDueDateBefore(MilestoneStatus status, LocalDate date);

    List<Milestone> findByStatusNotAndDueDate(MilestoneStatus status, LocalDate date);
}
