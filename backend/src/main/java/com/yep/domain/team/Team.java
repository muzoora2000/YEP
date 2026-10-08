package com.yep.domain.team;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.interfaces.Assessable;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.interfaces.Notifiable;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A small team of participants solving a real problem inside a Level Two
 * virtual exchange. Implements {@link Notifiable} (the whole team can be
 * notified), {@link Assessable} (teams are assessed) and
 * {@link Certifiable} (team projects generate certificates).
 */
@Entity
@Table(
        name = "teams",
        indexes = {
                @Index(name = "idx_teams_level_two_exchange_id", columnList = "level_two_exchange_id"),
                @Index(name = "idx_teams_mentor_id", columnList = "mentor_id"),
                @Index(name = "idx_teams_created_at", columnList = "created_at")
        }
)
public class Team extends BaseEntity implements Notifiable, Assessable, Certifiable {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "level_two_exchange_id", nullable = false)
    private LevelTwoExchange levelTwoExchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<TeamMember> teamMembers = new LinkedHashSet<>();

    @OneToOne(mappedBy = "team", fetch = FetchType.LAZY)
    private ImpactProject impactProject;

    @OneToMany(mappedBy = "teamTarget", fetch = FetchType.LAZY)
    private Set<Assessment> assessments = new LinkedHashSet<>();

    // ---------------------------------------------------------------------
    // Notifiable - in-app delivery to the whole roster
    // ---------------------------------------------------------------------

    @Override
    public String getNotificationAddress() {
        return "team:" + (getId() != null ? getId() : 0);
    }

    @Override
    public NotificationChannel getPreferredChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public String getNotificationLabel() {
        return "Team " + name;
    }

    // ---------------------------------------------------------------------
    // Assessable
    // ---------------------------------------------------------------------

    @Override
    public com.yep.domain.enums.AssessmentTargetType getAssessmentTargetType() {
        return com.yep.domain.enums.AssessmentTargetType.TEAM;
    }

    @Override
    public String getAssessmentLabel() {
        return name;
    }

    @Override
    public List<Participant> getAssessmentRecipients() {
        return memberParticipants().stream().toList();
    }

    // ---------------------------------------------------------------------
    // Certifiable
    // ---------------------------------------------------------------------

    @Override
    public String getCertificateTitle() {
        return "Impact Project Completion - " + name;
    }

    @Override
    public String getCertificateReference() {
        return impactProject != null ? impactProject.getTitle() : name;
    }

    @Override
    public boolean qualifiesForCertificate() {
        return impactProject != null && impactProject.isCompleted();
    }

    @Override
    public List<Participant> getCertificateRecipients() {
        return memberParticipants().stream().toList();
    }

    // ---------------------------------------------------------------------
    // Membership helpers (encapsulation of the join table)
    // ---------------------------------------------------------------------

    /**
     * Adds a membership to the in-memory roster. The returned {@link TeamMember}
     * is still transient: it has to be written through
     * {@code teamMemberRepository.save(...)} <em>before</em> the team itself is
     * merged again, otherwise Hibernate copies the collection without cascade
     * information and leaves a blank placeholder behind.
     */
    public TeamMember addMember(Participant participant, TeamMemberRole role) {
        if (isMember(participant)) {
            return null;
        }
        TeamMember membership = new TeamMember(this, participant, role);
        teamMembers.add(membership);
        return membership;
    }

    public void removeMember(Participant participant) {
        teamMembers.removeIf(tm -> Objects.equals(tm.getParticipant(), participant));
    }

    public boolean isMember(Participant participant) {
        return teamMembers.stream().anyMatch(tm -> Objects.equals(tm.getParticipant(), participant));
    }

    public boolean hasTeamLead() {
        return teamMembers.stream().anyMatch(tm -> tm.getMemberRole() == TeamMemberRole.LEAD);
    }

    public Participant findTeamLead() {
        return teamMembers.stream()
                .filter(tm -> tm.getMemberRole() == TeamMemberRole.LEAD)
                .map(TeamMember::getParticipant)
                .findFirst()
                .orElse(null);
    }

    public Set<Participant> memberParticipants() {
        return teamMembers.stream()
                .map(TeamMember::getParticipant)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public int memberCount() {
        return teamMembers.size();
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LevelTwoExchange getLevelTwoExchange() {
        return levelTwoExchange;
    }

    public void setLevelTwoExchange(LevelTwoExchange levelTwoExchange) {
        this.levelTwoExchange = levelTwoExchange;
    }

    public Mentor getMentor() {
        return mentor;
    }

    public void setMentor(Mentor mentor) {
        this.mentor = mentor;
    }

    public Set<TeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(Set<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public ImpactProject getImpactProject() {
        return impactProject;
    }

    public void setImpactProject(ImpactProject impactProject) {
        this.impactProject = impactProject;
    }

    public Set<Assessment> getAssessments() {
        return assessments;
    }

    public void setAssessments(Set<Assessment> assessments) {
        this.assessments = assessments;
    }
}
