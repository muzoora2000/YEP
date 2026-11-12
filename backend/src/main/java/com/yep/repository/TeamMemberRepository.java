package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.project.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamTeamId(Long teamId);

    List<TeamMember> findByYouthUserId(Long youthId);

    Optional<TeamMember> findByTeamTeamIdAndYouthUserId(Long teamId, Long youthId);

    boolean existsByTeamTeamIdAndYouthUserId(Long teamId, Long youthId);

    boolean existsByYouthUserIdAndTeamActivityActivityId(Long youthId, Long activityId);

    long countByTeamTeamId(Long teamId);
}
