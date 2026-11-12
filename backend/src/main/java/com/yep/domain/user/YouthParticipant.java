package com.yep.domain.user;

import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.learning.Assessment;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.project.Team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "youth_participant")
@PrimaryKeyJoinColumn(name = "youth_id")
public class YouthParticipant extends User {

    @Column(name = "age")
    private Integer age;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "digital_skills", length = 80)
    private String digitalSkills;

    @Column(name = "level_two_eligible", nullable = false)
    private boolean levelTwoEligible = false;

    @Column(name = "institution", length = 190)
    private String institution;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "share_profile_with_partners", nullable = false)
    private boolean shareProfileWithPartners = false;

    @Column(name = "consent_date")
    private OffsetDateTime consentDate;

    public YouthParticipant() {
        super();
        setRole(Role.PARTICIPANT);
    }

    /** Domain operation: a participant joins an activity; capacity policy is applied by the service. */
    public Enrollment enrollActivity(LearningActivity activity) {
        Enrollment enrollment = new Enrollment(this, activity);
        enrollment.confirmEnrollment();
        return enrollment;
    }

    /** Domain operation: join a Level Two team through the Team aggregate. */
    public void joinTeam(Team team) {
        team.addMember(this, TeamMemberRole.MEMBER);
    }

    /** Average percentage across graded assessments; useful for progress cards. */
    public double viewProgress(List<Assessment> assessments) {
        return assessments.stream()
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .mapToDouble(a -> (a.getScore() * 100.0) / a.getMaxScore())
                .average()
                .orElse(0.0);
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDigitalSkills() {
        return digitalSkills;
    }

    public void setDigitalSkills(String digitalSkills) {
        this.digitalSkills = digitalSkills;
    }

    public boolean isLevelTwoEligible() {
        return levelTwoEligible;
    }

    public void setLevelTwoEligible(boolean levelTwoEligible) {
        this.levelTwoEligible = levelTwoEligible;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isShareProfileWithPartners() {
        return shareProfileWithPartners;
    }

    public void setShareProfileWithPartners(boolean shareProfileWithPartners) {
        this.shareProfileWithPartners = shareProfileWithPartners;
    }

    public OffsetDateTime getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(OffsetDateTime consentDate) {
        this.consentDate = consentDate;
    }
}
