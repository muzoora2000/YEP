package com.yep.domain.user;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.OrganisationType;
import com.yep.domain.enums.Role;
import com.yep.domain.partner.Opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_organisation")
@PrimaryKeyJoinColumn(name = "partner_id")
public class PartnerOrganisation extends User {

    @Column(name = "organisation_name", nullable = false, length = 190)
    private String organisationName;

    @Column(name = "country", length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "organisation_type", length = 30)
    private OrganisationType organisationType;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    public PartnerOrganisation() {
        super();
        setRole(Role.PARTNER);
    }

    public LearningActivity supportActivities(LearningActivity activity) {
        activity.setPartner(this);
        return activity;
    }

    public Opportunity provideOpportunities(Opportunity opportunity) {
        opportunity.setPartner(this);
        return opportunity;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public OrganisationType getOrganisationType() {
        return organisationType;
    }

    public void setOrganisationType(OrganisationType organisationType) {
        this.organisationType = organisationType;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
