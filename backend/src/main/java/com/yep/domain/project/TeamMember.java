package com.yep.domain.project;

import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.user.YouthParticipant;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "team_member", uniqueConstraints = @UniqueConstraint(name = "uk_team_member_team_youth", columnNames = { "team_id", "youth_id" }))
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "youth_id", nullable = false)
    private YouthParticipant youth;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TeamMemberRole role;

    protected TeamMember() {
    }

    public TeamMember(Team team, YouthParticipant youth, TeamMemberRole role) {
        this.team = team;
        this.youth = youth;
        this.role = role;
    }

    public void assignRole(TeamMemberRole role) {
        this.role = role;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public YouthParticipant getYouth() {
        return youth;
    }

    public void setYouth(YouthParticipant youth) {
        this.youth = youth;
    }

    public TeamMemberRole getRole() {
        return role;
    }

    public void setRole(TeamMemberRole role) {
        this.role = role;
    }
}
