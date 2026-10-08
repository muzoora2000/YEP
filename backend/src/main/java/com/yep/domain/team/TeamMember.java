package com.yep.domain.team;

import java.time.OffsetDateTime;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.user.Participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Membership of a participant inside a team ({@code team_members} table).
 * Gives the many-to-many between {@code Team} and {@code Participant} its own
 * semantics (role, join date) instead of being a bare link table.
 */
@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_members_team_participant",
                columnNames = { "team_id", "participant_id" }
        ),
        indexes = {
                @Index(name = "idx_team_members_team_id", columnList = "team_id"),
                @Index(name = "idx_team_members_participant_id", columnList = "participant_id")
        }
)
public class TeamMember extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "member_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TeamMemberRole memberRole = TeamMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    public TeamMember() {
    }

    public TeamMember(Team team, Participant participant, TeamMemberRole role) {
        this.team = team;
        this.participant = participant;
        this.memberRole = role == null ? TeamMemberRole.MEMBER : role;
        this.joinedAt = OffsetDateTime.now();
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public TeamMemberRole getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(TeamMemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
