package com.yep.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.message.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByRecipientIdOrderBySentAtDesc(Long recipientId, Pageable pageable);

    List<Message> findByTeamIdOrderBySentAtDesc(Long teamId);

    List<Message> findByExchangeIdOrderBySentAtDesc(Long exchangeId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    boolean existsByIdAndRecipientId(Long id, Long recipientId);

    /** Direct, team and system traffic for one user. */
    @Query("""
            SELECT m FROM Message m
             WHERE m.recipient.id = :userId
                OR m.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.participant.id = :userId)
             ORDER BY m.sentAt DESC
            """)
    Page<Message> findInbox(@Param("userId") Long userId, Pageable pageable);

    /** Published announcements addressed to everyone. */
    @Query("""
            SELECT m FROM Announcement m
             WHERE m.published = true
               AND m.scope = com.yep.domain.enums.AnnouncementScope.ALL_USERS
             ORDER BY m.sentAt DESC
            """)
    Page<Message> findGlobalAnnouncements(Pageable pageable);

    /** Published announcements of the exchanges the user is enrolled in. */
    @Query("""
            SELECT m FROM Announcement m
             WHERE m.published = true
               AND m.exchange.id IN (
                    SELECT e.virtualExchange.id FROM Enrollment e WHERE e.participant.id = :userId)
             ORDER BY m.sentAt DESC
            """)
    Page<Message> findCohortAnnouncements(@Param("userId") Long userId, Pageable pageable);

    /** AI drafted announcements still waiting for a human decision. */
    @Query("SELECT m FROM Announcement m WHERE m.requiresHumanApproval = true ORDER BY m.sentAt DESC")
    List<Message> findPendingApproval();
}
