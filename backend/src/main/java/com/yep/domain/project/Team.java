package com.yep.domain.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.enums.TeamStatus;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @Column(name = "team_name", nullable = false, length = 120)
    private String teamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamStatus status = TeamStatus.OPEN;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    protected Team() {
    }

    public Team(LearningActivity activity, String teamName, User createdBy) {
        this.activity = activity;
        this.teamName = teamName;
        this.createdBy = createdBy;
    }

    public TeamMember addMember(YouthParticipant youth, TeamMemberRole role) {
        Optional<TeamMember> existing = members.stream()
                .filter(member -> member.getYouth().getUserId().equals(youth.getUserId()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        TeamMember member = new TeamMember(this, youth, role);
        members.add(member);
        return member;
    }

    public void removeMember(YouthParticipant youth) {
        members.removeIf(member -> member.getYouth().getUserId().equals(youth.getUserId()));
    }

    public boolean isMember(YouthParticipant youth) {
        return members.stream().anyMatch(member -> member.getYouth().getUserId().equals(youth.getUserId()));
    }

    public boolean isLeader(YouthParticipant youth) {
        return members.stream().anyMatch(member -> member.getYouth().getUserId().equals(youth.getUserId())
                && member.getRole() == TeamMemberRole.LEADER);
    }

    public int memberCount() {
        return members.size();
    }

    public void refreshStatus(int maxTeamSize) {
        if (status == TeamStatus.LOCKED) {
            return;
        }
        status = members.size() >= maxTeamSize ? TeamStatus.FULL : TeamStatus.OPEN;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Mentor getMentor() {
        return mentor;
    }

    public void setMentor(Mentor mentor) {
        this.mentor = mentor;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }
}
