package com.yep.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.OpportunityStatus;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SupportStatus;
import com.yep.domain.partner.Opportunity;
import com.yep.domain.partner.OpportunityInterest;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.ProjectSupport;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.PartnerDtos.InterestRequest;
import com.yep.dto.PartnerDtos.InterestResponse;
import com.yep.dto.PartnerDtos.OpportunityRequest;
import com.yep.dto.PartnerDtos.OpportunityResponse;
import com.yep.dto.PartnerDtos.PartnerSummary;
import com.yep.dto.PartnerDtos.SupportRequest;
import com.yep.dto.PartnerDtos.SupportResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.OpportunityInterestRepository;
import com.yep.repository.OpportunityRepository;
import com.yep.repository.PartnerOrganisationRepository;
import com.yep.repository.ProjectSupportRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

@Service
public class PartnerService {

    private final PartnerOrganisationRepository partnerRepository;
    private final OpportunityRepository opportunityRepository;
    private final OpportunityInterestRepository interestRepository;
    private final ProjectSupportRepository supportRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public PartnerService(PartnerOrganisationRepository partnerRepository, OpportunityRepository opportunityRepository,
            OpportunityInterestRepository interestRepository, ProjectSupportRepository supportRepository,
            UserRepository userRepository, ProjectService projectService, TeamService teamService,
            NotificationService notificationService, SecurityUtils securityUtils, AuditService auditService) {
        this.partnerRepository = partnerRepository;
        this.opportunityRepository = opportunityRepository;
        this.interestRepository = interestRepository;
        this.supportRepository = supportRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.teamService = teamService;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional
    public OpportunityResponse createOpportunity(OpportunityRequest request) {
        PartnerOrganisation partner = currentPartner();
        requireVerified(partner);
        Opportunity opportunity = new Opportunity(partner, request.title().trim(), request.opportunityType());
        opportunity.setDescription(trimToNull(request.description()));
        opportunity.setLocation(trimToNull(request.location()));
        opportunity.setDeadline(request.deadline());
        opportunity = opportunityRepository.save(opportunity);
        auditService.record(currentActor(), "OPPORTUNITY_CREATED", "Opportunity", opportunity.getOpportunityId(),
                opportunity.getTitle());
        return toOpportunityResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse updateOpportunity(Long opportunityId, OpportunityRequest request) {
        Opportunity opportunity = findOpportunity(opportunityId);
        requireOwnerOrAdmin(opportunity);
        opportunity.setTitle(request.title().trim());
        opportunity.setOpportunityType(request.opportunityType());
        opportunity.setDescription(trimToNull(request.description()));
        opportunity.setLocation(trimToNull(request.location()));
        opportunity.setDeadline(request.deadline());
        opportunityRepository.save(opportunity);
        auditService.record(currentActor(), "OPPORTUNITY_UPDATED", "Opportunity", opportunityId,
                opportunity.getTitle());
        return toOpportunityResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse closeOpportunity(Long opportunityId) {
        Opportunity opportunity = findOpportunity(opportunityId);
        requireOwnerOrAdmin(opportunity);
        opportunity.close();
        opportunityRepository.save(opportunity);
        auditService.record(currentActor(), "OPPORTUNITY_CLOSED", "Opportunity", opportunityId,
                opportunity.getTitle());
        return toOpportunityResponse(opportunity);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> publicOpportunities(Pageable pageable) {
        return opportunityRepository.findByStatus(OpportunityStatus.OPEN, pageable).map(this::toOpportunityResponse);
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> myOpportunities() {
        PartnerOrganisation partner = currentPartner();
        return opportunityRepository.findByPartnerUserIdOrderByCreatedAtDesc(partner.getUserId()).stream()
                .map(this::toOpportunityResponse).toList();
    }

    @Transactional
    public InterestResponse expressInterest(Long opportunityId, InterestRequest request) {
        YouthParticipant youth = currentParticipant();
        Opportunity opportunity = findOpportunity(opportunityId);
        if (opportunity.getStatus() != OpportunityStatus.OPEN) {
            throw new BusinessRuleException("OPPORTUNITY_CLOSED", "This opportunity is no longer open.");
        }
        if (opportunity.getDeadline() != null && opportunity.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("DEADLINE_PASSED", "The application deadline has passed.");
        }
        if (interestRepository.existsByOpportunityOpportunityIdAndYouthUserId(opportunityId, youth.getUserId())) {
            throw new ConflictException("ALREADY_EXPRESSED_INTEREST", "You have already expressed interest.");
        }
        OpportunityInterest interest = interestRepository.save(new OpportunityInterest(opportunity, youth,
                trimToNull(request.message())));
        notificationService.notify(opportunity.getPartner(),
                youth.getName() + " expressed interest in " + opportunity.getTitle(), "/partner/opportunities",
                NotificationType.IN_APP);
        auditService.record(currentActor(), "OPPORTUNITY_INTEREST", "Opportunity", opportunityId,
                youth.getName());
        return toInterestResponse(interest);
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> myInterests() {
        YouthParticipant youth = currentParticipant();
        return interestRepository.findByYouthUserId(youth.getUserId()).stream().map(this::toInterestResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> opportunityInterests(Long opportunityId) {
        Opportunity opportunity = findOpportunity(opportunityId);
        requireOwnerOrAdmin(opportunity);
        return interestRepository.findByOpportunityOpportunityIdOrderByCreatedAtDesc(opportunityId).stream()
                .map(this::toInterestResponse).toList();
    }

    @Transactional
    public SupportResponse offerSupport(Long projectId, SupportRequest request) {
        PartnerOrganisation partner = currentPartner();
        requireVerified(partner);
        ImpactProject project = projectService.find(projectId);
        if (!project.isOpenForSupport() || (project.getStatus() != ProjectStatus.APPROVED
                && project.getStatus() != ProjectStatus.COMPLETED)) {
            throw new BusinessRuleException("PROJECT_NOT_OPEN_FOR_SUPPORT",
                    "This project is not currently open to partner support.");
        }
        if (supportRepository.existsByProjectProjectIdAndPartnerUserIdAndSupportType(projectId,
                partner.getUserId(), request.supportType())) {
            throw new ConflictException("SUPPORT_ALREADY_OFFERED", "You already offered this type of support.");
        }
        ProjectSupport support = supportRepository.save(new ProjectSupport(project, partner,
                request.supportType(), trimToNull(request.message())));
        if (project.getTeam().getCreatedBy() != null) {
            notificationService.notify(project.getTeam().getCreatedBy(),
                    partner.getOrganisationName() + " offered " + request.supportType() + " support for "
                            + project.getTitle(),
                    "/projects/" + projectId, NotificationType.IN_APP);
        }
        auditService.record(currentActor(), "PROJECT_SUPPORT_OFFERED", "ImpactProject", projectId,
                request.supportType().name());
        return toSupportResponse(support);
    }

    @Transactional(readOnly = true)
    public List<SupportResponse> mySupports() {
        PartnerOrganisation partner = currentPartner();
        return supportRepository.findByPartnerUserIdOrderByCreatedAtDesc(partner.getUserId()).stream()
                .map(this::toSupportResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportResponse> projectSupports(Long projectId) {
        ImpactProject project = projectService.find(projectId);
        teamService.requireView(project.getTeam());
        return supportRepository.findByProjectProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toSupportResponse).toList();
    }

    @Transactional
    public SupportResponse respondToSupport(Long supportId, boolean accept) {
        ProjectSupport support = supportRepository.findById(supportId)
                .orElseThrow(() -> new NotFoundException("SUPPORT_NOT_FOUND", "Support offer not found."));
        teamService.requireView(support.getProject().getTeam());
        if (support.getStatus() != SupportStatus.OFFERED) {
            throw new BusinessRuleException("SUPPORT_ALREADY_RESOLVED", "This support offer is already resolved.");
        }
        if (accept) {
            support.accept();
            notificationService.notify(support.getPartner(),
                    support.getProject().getTeam().getTeamName() + " accepted your "
                            + support.getSupportType() + " support",
                    "/partner/support", NotificationType.IN_APP);
        } else {
            support.decline();
        }
        supportRepository.save(support);
        auditService.record(currentActor(), "PROJECT_SUPPORT_RESPONDED", "ProjectSupport", supportId,
                support.getStatus().name());
        return toSupportResponse(support);
    }

    @Transactional(readOnly = true)
    public Page<PartnerSummary> listPartners(Pageable pageable) {
        return partnerRepository.findAll(pageable).map(this::toPartnerSummary);
    }

    @Transactional
    public PartnerSummary verifyPartner(Long partnerId, boolean verified) {
        requireAdmin();
        PartnerOrganisation partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("PARTNER_NOT_FOUND", "Partner organisation not found."));
        partner.setVerified(verified);
        partnerRepository.save(partner);
        notificationService.notify(partner, verified ? "Your organisation has been verified."
                : "Your organisation verification was removed.", "/profile", NotificationType.IN_APP);
        auditService.record(currentActor(), "PARTNER_VERIFICATION_CHANGED", "PartnerOrganisation", partnerId,
                String.valueOf(verified));
        return toPartnerSummary(partner);
    }

    public Opportunity findOpportunity(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new NotFoundException("OPPORTUNITY_NOT_FOUND", "Opportunity not found."));
    }

    public OpportunityResponse toOpportunityResponse(Opportunity opportunity) {
        PartnerOrganisation partner = opportunity.getPartner();
        return new OpportunityResponse(opportunity.getOpportunityId(), partner.getUserId(), partner.getName(),
                partner.getOrganisationName(), opportunity.getTitle(), opportunity.getOpportunityType(),
                opportunity.getDescription(), opportunity.getLocation(), opportunity.getDeadline(),
                opportunity.getStatus(), opportunity.getCreatedAt(),
                interestRepository.countByOpportunityOpportunityId(opportunity.getOpportunityId()));
    }

    public InterestResponse toInterestResponse(OpportunityInterest interest) {
        return new InterestResponse(interest.getInterestId(), interest.getOpportunity().getOpportunityId(),
                interest.getOpportunity().getTitle(), interest.getYouth().getUserId(),
                interest.getYouth().getName(), interest.getYouth().getEmail(), interest.getMessage(),
                interest.getCreatedAt());
    }

    public SupportResponse toSupportResponse(ProjectSupport support) {
        return new SupportResponse(support.getSupportId(), support.getProject().getProjectId(),
                support.getProject().getTitle(), support.getPartner().getUserId(), support.getPartner().getName(),
                support.getPartner().getOrganisationName(), support.getSupportType(), support.getMessage(),
                support.getStatus(), support.getCreatedAt());
    }

    public PartnerSummary toPartnerSummary(PartnerOrganisation partner) {
        return new PartnerSummary(partner.getUserId(), partner.getName(), partner.getEmail(),
                partner.getOrganisationName(), partner.getCountry(), partner.getOrganisationType(),
                partner.getWebsite(), partner.getDescription(), partner.isVerified());
    }

    private PartnerOrganisation currentPartner() {
        User user = currentActor();
        if (!(user instanceof PartnerOrganisation partner)) {
            throw new ForbiddenException("PARTNER_ONLY", "Partner organisation account required.");
        }
        return partner;
    }

    private YouthParticipant currentParticipant() {
        User user = currentActor();
        if (!(user instanceof YouthParticipant youth)) {
            throw new ForbiddenException("PARTICIPANT_ONLY", "Youth participant account required.");
        }
        return youth;
    }

    private void requireVerified(PartnerOrganisation partner) {
        if (!partner.isVerified()) {
            throw new ForbiddenException("PARTNER_NOT_VERIFIED",
                    "Your organisation must be verified before publishing opportunities or offering support.");
        }
    }

    private void requireOwnerOrAdmin(Opportunity opportunity) {
        if (securityUtils.currentUser().role() == Role.ADMIN) {
            return;
        }
        if (!opportunity.getPartner().getUserId().equals(securityUtils.currentUserId())) {
            throw new ForbiddenException("FORBIDDEN", "Only the owning partner can manage this opportunity.");
        }
    }

    private void requireAdmin() {
        if (securityUtils.currentUser().role() != Role.ADMIN) {
            throw new ForbiddenException("ADMIN_ONLY", "Administrator account required.");
        }
    }

    private User currentActor() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
