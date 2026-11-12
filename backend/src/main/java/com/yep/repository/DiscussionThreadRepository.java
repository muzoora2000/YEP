package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.learning.DiscussionThread;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, Long> {

    List<DiscussionThread> findByActivityActivityId(Long activityId);

    Optional<DiscussionThread> findByTeamTeamId(Long teamId);

    Optional<DiscussionThread> findFirstByActivityActivityIdAndTeamIsNull(Long activityId);

    Optional<DiscussionThread> findFirstByActivityActivityIdAndTeamIsNullAndTopicIgnoreCase(Long activityId,
            String topic);
}
