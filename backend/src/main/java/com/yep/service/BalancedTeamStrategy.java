package com.yep.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.project.Team;
import com.yep.domain.user.YouthParticipant;
import com.yep.exception.BusinessRuleException;

/**
 * Strategy Pattern (Concrete): distributes participants into balanced teams,
 * mixing countries and digital-skill levels and keeping team sizes even.
 */
@Component("balancedTeamStrategy")
public class BalancedTeamStrategy implements TeamFormationStrategy {

    @Override
    public List<Team> formTeams(List<YouthParticipant> unassigned, LearningActivity activity) {
        int total = unassigned.size();
        if (total == 0) {
            return List.of();
        }
        int maxSize = Math.max(1, activity.getMaxTeamSize());
        int minSize = Math.max(1, activity.getMinTeamSize());
        if (total < minSize) {
            throw new BusinessRuleException("NOT_ENOUGH_PARTICIPANTS",
                    "At least " + minSize + " participants are needed to form a team.");
        }
        // Team count keeps every team within [minSize, maxSize] and sizes even.
        int byCapacity = (total + maxSize - 1) / maxSize;   // ceil(total / max)
        int byMinimum = Math.max(1, total / minSize);       // floor(total / min)
        int teamCount = Math.max(1, Math.min(byCapacity, byMinimum));

        // Sort by country then digital-skill level so equal profiles sit together;
        // round-robin dealing then spreads each country/skill across different teams.
        List<YouthParticipant> sorted = new ArrayList<>(unassigned);
        sorted.sort((a, b) -> {
            int byCountry = safeStr(a.getCountry()).compareTo(safeStr(b.getCountry()));
            return byCountry != 0 ? byCountry
                    : safeStr(a.getDigitalSkills()).compareTo(safeStr(b.getDigitalSkills()));
        });

        List<List<YouthParticipant>> buckets = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            buckets.add(new ArrayList<>());
        }
        int next = 0;
        for (YouthParticipant youth : sorted) {
            buckets.get(next % teamCount).add(youth); // dealing round-robin mixes profiles
            next++;
        }

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            Team team = new Team(activity, "Team " + (i + 1), activity.getFacilitator());
            boolean first = true;
            for (YouthParticipant youth : buckets.get(i)) {
                // Encapsulation: membership changes only through Team.addMember().
                team.addMember(youth, first ? TeamMemberRole.LEADER : TeamMemberRole.MEMBER);
                first = false;
            }
            team.refreshStatus(maxSize);
            teams.add(team);
        }
        return teams;
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }
}
