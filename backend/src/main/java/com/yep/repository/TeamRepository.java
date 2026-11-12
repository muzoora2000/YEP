package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.project.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByActivityActivityId(Long activityId);

    List<Team> findByMentorUserId(Long mentorId);

    Optional<Team> findFirstByActivityActivityIdAndTeamNameIgnoreCase(Long activityId, String teamName);

    long countByMentorUserId(Long mentorId);

    boolean existsByActivityActivityIdAndMentorUserId(Long activityId, Long mentorUserId);
}
