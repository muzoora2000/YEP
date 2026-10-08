package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.team.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByLevelTwoExchangeId(Long exchangeId);

    Optional<Team> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT t FROM Team t JOIN t.teamMembers tm WHERE tm.participant.id = :participantId")
    List<Team> findByParticipant(@Param("participantId") Long participantId);

    @Query("SELECT t FROM Team t WHERE t.mentor.id = :mentorId")
    List<Team> findByMentor(@Param("mentorId") Long mentorId);
}
