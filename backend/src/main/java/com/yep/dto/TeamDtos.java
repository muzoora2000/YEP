package com.yep.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.team.Team;
import com.yep.domain.team.TeamMember;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contracts for {@code /api/v1/teams}. */
public final class TeamDtos {

    private TeamDtos() {
    }

    public record TeamRequest(
            @NotBlank(message = "team name is required")
            @Size(max = 120)
            String name,

            @Size(max = 2000)
            String description,

            @NotNull(message = "level two exchange id is required")
            Long levelTwoExchangeId,
            Long mentorId) {
    }

    public record TeamMemberResponse(
            Long participantId,
            String fullName,
            String email,
            TeamMemberRole memberRole,
            OffsetDateTime joinedAt) {

        public static TeamMemberResponse of(TeamMember membership) {
            return new TeamMemberResponse(
                    membership.getParticipant() != null ? membership.getParticipant().getId() : null,
                    membership.getParticipant() != null ? membership.getParticipant().getFullName() : null,
                    membership.getParticipant() != null ? membership.getParticipant().getEmail() : null,
                    membership.getMemberRole(),
                    membership.getJoinedAt());
        }
    }

    public record TeamResponse(
            Long id,
            String name,
            String description,
            Long levelTwoExchangeId,
            String exchangeTitle,
            Long mentorId,
            String mentorName,
            List<TeamMemberResponse> members,
            int memberCount,
            Long impactProjectId,
            String impactProjectTitle,
            String impactProjectStatus,
            OffsetDateTime createdAt) {

        public static TeamResponse of(Team team) {
            ImpactProject project = team.getImpactProject();
            return new TeamResponse(
                    team.getId(),
                    team.getName(),
                    team.getDescription(),
                    team.getLevelTwoExchange() != null ? team.getLevelTwoExchange().getId() : null,
                    team.getLevelTwoExchange() != null ? team.getLevelTwoExchange().getTitle() : null,
                    team.getMentor() != null ? team.getMentor().getId() : null,
                    team.getMentor() != null ? team.getMentor().getFullName() : null,
                    team.getTeamMembers().stream().map(TeamMemberResponse::of).toList(),
                    team.memberCount(),
                    project != null ? project.getId() : null,
                    project != null ? project.getTitle() : null,
                    project != null ? project.getStatus().name() : null,
                    team.getCreatedAt());
        }
    }

    public record AddMemberRequest(
            @NotNull(message = "participant id is required")
            Long participantId,
            TeamMemberRole memberRole) {
    }

    public record AssignMentorRequest(@NotNull Long mentorId) {
    }
}
