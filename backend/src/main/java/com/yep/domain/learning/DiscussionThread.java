package com.yep.domain.learning;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.project.Team;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "discussion_thread")
public class DiscussionThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thread_id")
    private Long threadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "topic", nullable = false, length = 190)
    private String topic;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscussionPost> posts = new ArrayList<>();

    protected DiscussionThread() {
    }

    public DiscussionThread(LearningActivity activity, String topic) {
        this.activity = activity;
        this.topic = topic;
        createThread();
    }

    public DiscussionThread(LearningActivity activity, Team team, String topic) {
        this(activity, topic);
        this.team = team;
    }

    public void createThread() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public void addPost(DiscussionPost post) {
        posts.add(post);
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DiscussionPost> getPosts() {
        return posts;
    }

    public void setPosts(List<DiscussionPost> posts) {
        this.posts = posts;
    }
}
