package com.yep.domain.exchange;

import java.util.LinkedHashSet;
import java.util.Set;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.domain.team.Team;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

/**
 * Level Two virtual exchange: small teams building Impact Projects that solve a
 * real problem in one of the platform focus areas.
 */
@Entity
@DiscriminatorValue("LEVEL_TWO")
public class LevelTwoExchange extends VirtualExchange {

    @Column(name = "focus_area", length = 50)
    @Enumerated(EnumType.STRING)
    private LevelTwoFocusArea focusArea;

    @Column(name = "max_teams")
    private Integer maxTeams;

    /** Application based admission: participants apply, facilitators select. */
    @Column(name = "applications_open")
    private boolean applicationsOpen = true;

    @OneToMany(mappedBy = "levelTwoExchange", fetch = FetchType.LAZY)
    private Set<Team> teams = new LinkedHashSet<>();

    @Override
    public ExchangeLevel getLevel() {
        return ExchangeLevel.LEVEL_TWO;
    }

    @Override
    public String getSummaryLabel() {
        return "Level Two - " + focusArea.name().replace('_', ' ');
    }

    @Override
    public boolean isAtCapacity() {
        if (maxTeams == null || maxTeams <= 0) {
            return false;
        }
        return teams != null && teams.size() >= maxTeams;
    }

    public boolean hasRoomForTeam() {
        return !isAtCapacity();
    }

    public boolean isTeamRegistered(Team team) {
        return teams != null && teams.contains(team);
    }

    public LevelTwoFocusArea getFocusArea() {
        return focusArea;
    }

    public void setFocusArea(LevelTwoFocusArea focusArea) {
        this.focusArea = focusArea;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(Integer maxTeams) {
        this.maxTeams = maxTeams;
    }

    public boolean isApplicationsOpen() {
        return applicationsOpen;
    }

    public void setApplicationsOpen(boolean applicationsOpen) {
        this.applicationsOpen = applicationsOpen;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }
}
