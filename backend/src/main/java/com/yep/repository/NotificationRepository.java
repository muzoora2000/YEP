package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByDateSentDesc(Long userId);

    List<Notification> findByRecipientUserIdAndReadFalseOrderByDateSentDesc(Long userId);

    long countByRecipientUserIdAndReadFalse(Long userId);
}
