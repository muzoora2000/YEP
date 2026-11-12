package com.yep.service;

import java.util.List;
import com.yep.domain.project.Team;
import com.yep.domain.user.YouthParticipant;
import com.yep.domain.activity.LearningActivity;

/** Strategy Pattern: pluggable team-formation algorithms. */
public interface TeamFormationStrategy {
    List<Team> formTeams(List<YouthParticipant> unassigned, LearningActivity activity);
}
