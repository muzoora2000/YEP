package com.yep.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.YouthParticipant;

public interface YouthParticipantRepository extends JpaRepository<YouthParticipant, Long> {

    List<YouthParticipant> findByCountry(String country);

    List<YouthParticipant> findByLevelTwoEligibleTrue();

    List<YouthParticipant> findByShareProfileWithPartnersTrue();

    Page<YouthParticipant> findByShareProfileWithPartnersTrue(Pageable pageable);

    long countByCountry(String country);
}
