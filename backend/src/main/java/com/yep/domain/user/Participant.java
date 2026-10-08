package com.yep.domain.user;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.yep.domain.certificate.Certificate;
import com.yep.domain.enums.AssessmentTargetType;
import com.yep.domain.enums.CompletionStatus;
import com.yep.domain.enums.SkillLevel;
import com.yep.domain.exchange.Enrollment;
import com.yep.domain.interfaces.Assessable;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.team.Team;
import com.yep.domain.team.TeamMember;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

/**
 * A young person (18-35) taking part in virtual exchanges.
 * Implements both polymorphic contracts of the platform:
 * a participant can be {@link Assessable assessed} and can hold {@link Certifiable certificates}.
 */
@Entity
@DiscriminatorValue("PARTICIPANT")
public class Participant extends User implements Assessable, Certifiable {

    @Column(name = "age")
    private Integer age;

    @Column(name = "skill_level", length = 30)
    @Enumerated(EnumType.STRING)
    private SkillLevel skillLevel = SkillLevel.BEGINNER;

    @OneToMany(mappedBy = "participant", fetch = FetchType.LAZY)
    private Set<Enrollment> enrolledActivities = java.util.Collections.emptySet();

    @OneToMany(mappedBy = "participant", fetch = FetchType.LAZY)
    private Set<TeamMember> teamMemberships = java.util.Collections.emptySet();

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    private Set<Certificate> certificates = java.util.Collections.emptySet();

    @Override
    public AssessmentTargetType getAssessmentTargetType() {
        return AssessmentTargetType.PARTICIPANT;
    }

    @Override
    public String getAssessmentLabel() {
        return getFullName();
    }

    @Override
    public List<Participant> getAssessmentRecipients() {
        return List.of(this);
    }

    @Override
    public String getCertificateTitle() {
        return "Certificate of Participation - " + getFullName();
    }

    @Override
    public String getCertificateReference() {
        return "YEP Virtual Exchange Participation";
    }

    @Override
    public boolean qualifiesForCertificate() {
        return enrolledActivities != null
                && enrolledActivities.stream()
                        .anyMatch(e -> e.getCompletionStatus() == CompletionStatus.COMPLETED);
    }

    @Override
    public List<Participant> getCertificateRecipients() {
        return List.of(this);
    }

    /** Derived view of the level two teams this participant belongs to. */
    public Set<Team> getTeams() {
        if (teamMemberships == null) {
            return Set.of();
        }
        return teamMemberships.stream()
                .map(TeamMember::getTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean isMemberOf(Team team) {
        return teamMemberships != null
                && teamMemberships.stream().anyMatch(tm -> Objects.equals(tm.getTeam(), team));
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public Set<Enrollment> getEnrolledActivities() {
        return enrolledActivities;
    }

    public void setEnrolledActivities(Set<Enrollment> enrolledActivities) {
        this.enrolledActivities = enrolledActivities;
    }

    public Set<TeamMember> getTeamMemberships() {
        return teamMemberships;
    }

    public void setTeamMemberships(Set<TeamMember> teamMemberships) {
        this.teamMemberships = teamMemberships;
    }

    public Set<Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(Set<Certificate> certificates) {
        this.certificates = certificates;
    }
}
