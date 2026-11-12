package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.OrganisationType;
import com.yep.domain.user.PartnerOrganisation;

public interface PartnerOrganisationRepository extends JpaRepository<PartnerOrganisation, Long> {

    List<PartnerOrganisation> findByVerifiedTrue();

    List<PartnerOrganisation> findByOrganisationType(OrganisationType organisationType);

    List<PartnerOrganisation> findByOrganisationNameIgnoreCase(String organisationName);
}
