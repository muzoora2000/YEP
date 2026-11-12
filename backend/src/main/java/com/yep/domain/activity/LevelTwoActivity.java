package com.yep.domain.activity;

import java.util.List;

import com.yep.domain.enums.ActivityLevel;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TWO")
public class LevelTwoActivity extends LearningActivity {

    public LevelTwoActivity() {
        super();
    }

    @Override
    public List<String> getLevelFeatures() {
        return List.of(
                "Selected participants only",
                "Team-based impact projects",
                "Mentorship and project review",
                "Responsible AI declaration",
                "Employment, education, agriculture, health, climate and digital inclusion");
    }

    @Override
    public boolean supportsTeamFormation() {
        return true;
    }

    @Override
    public ActivityLevel getLevel() {
        return ActivityLevel.TWO;
    }

    public String formTeams() {
        return "Team formation enabled for " + getTitle();
    }

    public String developImpactProjects() {
        return "Impact project workspace enabled for " + getTitle();
    }
}
