package com.yep.domain.activity;

import java.util.List;

import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.learning.DiscussionThread;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ONE")
public class LevelOneActivity extends LearningActivity {

    public LevelOneActivity() {
        super();
    }

    @Override
    public List<String> getLevelFeatures() {
        return List.of(
                "Large-group online learning",
                "Digital literacy",
                "AI literacy",
                "Critical thinking",
                "Media literacy",
                "Intercultural communication");
    }

    @Override
    public boolean supportsTeamFormation() {
        return false;
    }

    @Override
    public ActivityLevel getLevel() {
        return ActivityLevel.ONE;
    }

    public String conductGroupLearning() {
        return "Group learning session for " + getTitle();
    }

    public DiscussionThread facilitateDiscussion() {
        return new DiscussionThread(this, "General");
    }
}
