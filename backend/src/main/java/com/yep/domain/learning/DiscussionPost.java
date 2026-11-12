package com.yep.domain.learning;

import java.time.OffsetDateTime;

import com.yep.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "discussion_post")
public class DiscussionPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private DiscussionThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    protected DiscussionPost() {
    }

    public DiscussionPost(DiscussionThread thread, User author, String content) {
        this.thread = thread;
        this.author = author;
        this.content = content;
        publishPost();
    }

    public void publishPost() {
        this.postedAt = OffsetDateTime.now();
    }

    public void editPost(String content) {
        this.content = content;
    }

    public void flag(String reason) {
        this.flagged = true;
        this.flagReason = reason;
    }

    public void hide() {
        this.hidden = true;
    }

    public void unhide() {
        this.hidden = false;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public DiscussionThread getThread() {
        return thread;
    }

    public void setThread(DiscussionThread thread) {
        this.thread = thread;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(OffsetDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
