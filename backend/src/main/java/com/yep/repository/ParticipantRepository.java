package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.Participant;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByCountry(String country);
}
