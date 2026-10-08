package com.yep.domain.user;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.yep.domain.assessment.Assessment;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

/**
 * A person who designs and runs Level One virtual exchange sessions.
 *
 * <p>Expertise topics are stored as a comma separated value on the shared
 * {@code users} table so the physical schema stays exactly as specified.</p>
 */
@Entity
@DiscriminatorValue("FACILITATOR")
public class Facilitator extends User {

    @Column(name = "expertise_area", length = 120)
    private String expertiseArea;

    @Column(name = "expertise_topics", length = 400)
    private String expertiseTopics;

    /**
     * Dev profile builds the schema from this mapping ({@code ddl-auto: update}),
     * so the column must carry the same {@code DEFAULT 0} as the Flyway baseline -
     * sibling roles (participant/mentor/admin) simply omit the column on insert.
     */
    @Column(name = "sessions_facilitated", nullable = false, columnDefinition = "integer not null default 0")
    private int sessionsFacilitated = 0;

    @OneToMany(mappedBy = "assessor", fetch = FetchType.LAZY)
    private Set<Assessment> assessmentsGiven = new LinkedHashSet<>();

    public void recordSessionFacilitated() {
        this.sessionsFacilitated = this.sessionsFacilitated + 1;
    }

    public boolean canFacilitate(String topic) {
        return expertiseTopics == null
                || expertiseTopics.isBlank()
                || topicSet().contains(topic.toUpperCase());
    }

    public Set<String> topicSet() {
        if (expertiseTopics == null || expertiseTopics.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(expertiseTopics.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void addTopic(String topic) {
        Set<String> topics = new LinkedHashSet<>(topicSet());
        if (topics.add(topic.toUpperCase())) {
            this.expertiseTopics = String.join(",", topics);
        }
    }

    public String getExpertiseArea() {
        return expertiseArea;
    }

    public void setExpertiseArea(String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    public String getExpertiseTopics() {
        return expertiseTopics;
    }

    public void setExpertiseTopics(String expertiseTopics) {
        this.expertiseTopics = expertiseTopics;
    }

    public int getSessionsFacilitated() {
        return sessionsFacilitated;
    }

    public void setSessionsFacilitated(int sessionsFacilitated) {
        this.sessionsFacilitated = sessionsFacilitated;
    }

    public Set<Assessment> getAssessmentsGiven() {
        return assessmentsGiven;
    }

    public void setAssessmentsGiven(Set<Assessment> assessmentsGiven) {
        this.assessmentsGiven = assessmentsGiven;
    }
}
