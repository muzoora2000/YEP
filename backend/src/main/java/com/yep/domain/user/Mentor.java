package com.yep.domain.user;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.yep.domain.team.Team;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

/**
 * An industry / academic mentor guiding level two teams.
 *
 * <p>Mentorship areas are stored as a comma separated value on the shared
 * {@code users} table so the physical schema stays exactly as specified.</p>
 */
@Entity
@DiscriminatorValue("MENTOR")
public class Mentor extends User {

    @Column(name = "mentorship_areas", length = 400)
    private String mentorshipAreas;

    @OneToMany(mappedBy = "mentor", fetch = FetchType.LAZY)
    private Set<Team> menteeTeams = new LinkedHashSet<>();

    public boolean guides(Team team) {
        return menteeTeams != null && menteeTeams.contains(team);
    }

    public boolean covers(String area) {
        if (mentorshipAreas == null || mentorshipAreas.isBlank()) {
            return true;
        }
        return areaSet().contains(area.toUpperCase());
    }

    public Set<String> areaSet() {
        if (mentorshipAreas == null || mentorshipAreas.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(mentorshipAreas.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String getMentorshipAreas() {
        return mentorshipAreas;
    }

    public void setMentorshipAreas(String mentorshipAreas) {
        this.mentorshipAreas = mentorshipAreas;
    }

    public Set<Team> getMenteeTeams() {
        return menteeTeams;
    }

    public void setMenteeTeams(Set<Team> menteeTeams) {
        this.menteeTeams = menteeTeams;
    }
}
