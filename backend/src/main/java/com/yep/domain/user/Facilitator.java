package com.yep.domain.user;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.Role;
import com.yep.domain.learning.Assessment;
import com.yep.domain.learning.DiscussionThread;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "facilitator")
@PrimaryKeyJoinColumn(name = "facilitator_id")
public class Facilitator extends User {

    @Column(name = "specialisation", length = 190)
    private String specialisation;

    public Facilitator() {
        super();
        setRole(Role.FACILITATOR);
    }

    public LearningActivity manageActivity(LearningActivity activity) {
        activity.setFacilitator(this);
        return activity;
    }

    public DiscussionThread guideParticipants(LearningActivity activity, String topic) {
        return new DiscussionThread(activity, topic);
    }

    public Assessment assessProgress(Assessment assessment, int score, String feedback) {
        assessment.evaluate(score, feedback);
        return assessment;
    }

    public String getSpecialisation() {
        return specialisation;
    }

    public void setSpecialisation(String specialisation) {
        this.specialisation = specialisation;
    }
}
