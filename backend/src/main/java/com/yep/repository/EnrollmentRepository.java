package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.exchange.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByParticipantIdAndVirtualExchangeId(Long participantId, Long exchangeId);

    boolean existsByParticipantIdAndVirtualExchangeId(Long participantId, Long exchangeId);

    List<Enrollment> findByParticipantId(Long participantId);

    List<Enrollment> findByVirtualExchangeId(Long exchangeId);

    long countByVirtualExchangeId(Long exchangeId);
}
