package com.yep.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.learning.DiscussionPost;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

    List<DiscussionPost> findByThreadThreadIdOrderByPostedAtAsc(Long threadId);

    List<DiscussionPost> findByThreadThreadIdAndHiddenFalseOrderByPostedAtAsc(Long threadId);

    List<DiscussionPost> findByFlaggedTrueAndHiddenFalseOrderByPostedAtAsc();

    long countByFlaggedTrueAndHiddenFalse();

    @Query("select max(p.postedAt) from DiscussionPost p where p.author.userId = :youthId and p.thread.activity.activityId = :activityId")
    Optional<OffsetDateTime> findLastPostDate(@Param("youthId") Long youthId, @Param("activityId") Long activityId);
}
