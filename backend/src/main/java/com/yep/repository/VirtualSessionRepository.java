package com.yep.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.SessionStatus;
import com.yep.domain.learning.VirtualSession;

public interface VirtualSessionRepository extends JpaRepository<VirtualSession, Long> {

    List<VirtualSession> findByActivityActivityIdOrderByScheduledStartAsc(Long activityId);

    List<VirtualSession> findByActivityActivityIdAndStatus(Long activityId, SessionStatus status);

    List<VirtualSession> findByStatusAndScheduledStartBetween(SessionStatus status, OffsetDateTime from, OffsetDateTime to);

    long countByActivityActivityIdAndStatusNot(Long activityId, SessionStatus status);

    long countByActivityActivityIdAndStatus(Long activityId, SessionStatus status);
}
