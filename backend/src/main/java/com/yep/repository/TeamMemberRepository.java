package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.team.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamIdAndParticipantId(Long teamId, Long participantId);

    boolean existsByTeamIdAndParticipantId(Long teamId, Long participantId);

    List<TeamMember> findByTeamId(Long teamId);

    List<TeamMember> findByParticipantId(Long participantId);

    void deleteByTeamIdAndParticipantId(Long teamId, Long participantId);
}
