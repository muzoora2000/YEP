package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.enums.AssessmentTargetType;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByParticipantTargetId(Long participantId);

    List<Assessment> findByTeamTargetId(Long teamId);

    List<Assessment> findByAssessorId(Long assessorId);

    List<Assessment> findByTargetType(AssessmentTargetType targetType);
}
