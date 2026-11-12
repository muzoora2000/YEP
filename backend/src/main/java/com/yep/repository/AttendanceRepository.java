package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.learning.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySessionSessionId(Long sessionId);

    long countBySessionSessionId(Long sessionId);

    List<Attendance> findByYouthUserId(Long youthId);

    Optional<Attendance> findBySessionSessionIdAndYouthUserId(Long sessionId, Long youthId);

    long countByYouthUserIdAndStatusIn(Long youthId, List<com.yep.domain.enums.AttendanceStatus> statuses);

    long countByYouthUserIdAndSessionActivityActivityIdAndStatusIn(Long youthId, Long activityId,
            List<com.yep.domain.enums.AttendanceStatus> statuses);

    long countByYouthUserIdAndSessionActivityActivityId(Long youthId, Long activityId);

    long countByYouthUserIdAndSessionActivityActivityIdAndSessionStatusInAndStatusIn(Long youthId,
            Long activityId, List<com.yep.domain.enums.SessionStatus> sessionStatuses,
            List<com.yep.domain.enums.AttendanceStatus> attendanceStatuses);
}
