package com.yep.service;

import org.springframework.stereotype.Component;

import com.yep.domain.user.Administrator;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.AuthDtos.UserProfile;

@Component
public class UserMapper {

    public UserProfile toProfile(User user) {
        Integer age = null;
        String country = null;
        String digitalSkills = null;
        boolean levelTwoEligible = false;
        String institution = null;
        String bio = null;
        boolean shareProfileWithPartners = false;
        java.time.OffsetDateTime consentDate = null;
        String specialisation = null;
        String organisation = null;
        String expertise = null;
        String department = null;
        String organisationName = null;
        com.yep.domain.enums.OrganisationType organisationType = null;
        String website = null;
        String description = null;
        boolean verified = false;

        if (user instanceof YouthParticipant participant) {
            age = participant.getAge();
            country = participant.getCountry();
            digitalSkills = participant.getDigitalSkills();
            levelTwoEligible = participant.isLevelTwoEligible();
            institution = participant.getInstitution();
            bio = participant.getBio();
            shareProfileWithPartners = participant.isShareProfileWithPartners();
            consentDate = participant.getConsentDate();
        } else if (user instanceof Facilitator facilitator) {
            specialisation = facilitator.getSpecialisation();
        } else if (user instanceof Mentor mentor) {
            organisation = mentor.getOrganisation();
            expertise = mentor.getExpertise();
        } else if (user instanceof Administrator administrator) {
            department = administrator.getDepartment();
        } else if (user instanceof PartnerOrganisation partner) {
            organisationName = partner.getOrganisationName();
            country = partner.getCountry();
            organisationType = partner.getOrganisationType();
            website = partner.getWebsite();
            description = partner.getDescription();
            verified = partner.isVerified();
        }

        return new UserProfile(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
                user.getPhone(), user.getCreatedAt(), user.getLastLoginAt(), age, country, digitalSkills,
                levelTwoEligible, institution, bio, shareProfileWithPartners, consentDate, specialisation,
                organisation, expertise, department, organisationName, organisationType, website, description, verified);
    }
}
