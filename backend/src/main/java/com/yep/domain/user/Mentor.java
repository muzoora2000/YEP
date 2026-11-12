package com.yep.domain.user;

import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.enums.Role;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.ProjectReview;
import com.yep.domain.project.Team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "mentor")
@PrimaryKeyJoinColumn(name = "mentor_id")
public class Mentor extends User {

    @Column(name = "organisation", length = 190)
    private String organisation;

    @Column(name = "expertise", length = 500)
    private String expertise;

    public Mentor() {
        super();
        setRole(Role.MENTOR);
    }

    public void mentorTeam(Team team) {
        team.setMentor(this);
    }

    public ProjectReview provideFeedback(ImpactProject project, ReviewDecision decision, String feedback) {
        return new ProjectReview(project, this, decision, feedback);
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }
}
