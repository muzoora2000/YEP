package com.yep.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.OpportunityStatus;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.platform.AuditLog;
import com.yep.domain.platform.SystemSetting;
import com.yep.dto.PartnerDtos.AuditLogResponse;
import com.yep.dto.PartnerDtos.DashboardResponse;
import com.yep.dto.PartnerDtos.PublicSettingsResponse;
import com.yep.dto.PartnerDtos.SettingResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AuditLogRepository;
import com.yep.repository.CertificateRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.LearningActivityRepository;
import com.yep.repository.OpportunityRepository;
import com.yep.repository.PartnerOrganisationRepository;
import com.yep.repository.SystemSettingRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.YouthParticipantRepository;
import com.yep.security.SecurityUtils;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class AdminPlatformService {

    private final UserRepository userRepository;
    private final LearningActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final ImpactProjectRepository projectRepository;
    private final CertificateRepository certificateRepository;
    private final OpportunityRepository opportunityRepository;
    private final PartnerOrganisationRepository partnerRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemSettingRepository settingRepository;
    private final SettingsService settingsService;
    private final SecurityUtils securityUtils;
    private final YouthParticipantRepository youthRepository;

    public AdminPlatformService(UserRepository userRepository, LearningActivityRepository activityRepository,
            EnrollmentRepository enrollmentRepository, TeamRepository teamRepository,
            ImpactProjectRepository projectRepository, CertificateRepository certificateRepository,
            OpportunityRepository opportunityRepository, PartnerOrganisationRepository partnerRepository,
            AuditLogRepository auditLogRepository, SystemSettingRepository settingRepository,
            SettingsService settingsService, SecurityUtils securityUtils,
            YouthParticipantRepository youthRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.certificateRepository = certificateRepository;
        this.opportunityRepository = opportunityRepository;
        this.partnerRepository = partnerRepository;
        this.auditLogRepository = auditLogRepository;
        this.settingRepository = settingRepository;
        this.settingsService = settingsService;
        this.securityUtils = securityUtils;
        this.youthRepository = youthRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(userRepository.count(),
                userRepository.countByRole(Role.PARTICIPANT),
                userRepository.countByRole(Role.FACILITATOR),
                userRepository.countByRole(Role.MENTOR),
                userRepository.countByRole(Role.PARTNER),
                userRepository.countByRole(Role.ADMIN),
                userRepository.countByStatus(AccountStatus.PENDING),
                userRepository.countByStatus(AccountStatus.ACTIVE),
                activityRepository.count(),
                activityRepository.countByStatus(ActivityStatus.PUBLISHED),
                enrollmentRepository.count(),
                teamRepository.count(),
                projectRepository.count(),
                projectRepository.countByStatus(ProjectStatus.APPROVED),
                certificateRepository.count(),
                opportunityRepository.count(),
                partnerRepository.findByVerifiedTrue().size());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> auditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> settings() {
        return settingRepository.findAll(Sort.by("settingKey")).stream().map(this::toSettingResponse).toList();
    }

    @Transactional
    public SettingResponse updateSetting(String key, String value) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new NotFoundException("SETTING_NOT_FOUND", "Setting not found."));
        setting.update(value.trim(), securityUtils.currentUserId());
        settingRepository.save(setting);
        return toSettingResponse(setting);
    }

    @Transactional(readOnly = true)
    public PublicSettingsResponse publicSettings() {
        return new PublicSettingsResponse(settingsService.value(SettingsService.PLATFORM_NAME,
                "YEP - Youth Empowerment Platform"), settingsService.countryList());
    }

    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        return new AuditLogResponse(log.getLogId(), log.getUser() == null ? null : log.getUser().getUserId(),
                log.getUser() == null ? null : log.getUser().getName(), log.getAction(), log.getEntity(),
                log.getEntityId(), log.getDetails(), log.getCreatedAt());
    }

    private SettingResponse toSettingResponse(SystemSetting setting) {
        return new SettingResponse(setting.getSettingKey(), setting.getSettingValue(), setting.getUpdatedBy(),
                setting.getUpdatedAt());
    }

    /**
     * Strategy pattern: report type drives which repository query and format runs.
     * Supported types: participants, enrollments, certificates, projects, opportunities.
     * Format "json" returns a List; format "csv" writes directly to the HTTP response.
     */
    @Transactional(readOnly = true)
    public Object report(String type, String format, HttpServletResponse response) {
        List<Map<String, Object>> rows = switch (type.toLowerCase()) {
            case "participants" -> youthRepository.findAll().stream().map(y -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("userId", y.getUserId());
                row.put("name", y.getName());
                row.put("email", y.getEmail());
                row.put("country", y.getCountry());
                row.put("age", y.getAge());
                row.put("digitalSkills", y.getDigitalSkills());
                row.put("levelTwoEligible", y.isLevelTwoEligible());
                row.put("status", y.getStatus());
                row.put("createdAt", y.getCreatedAt());
                return row;
            }).toList();
            case "enrollments" -> enrollmentRepository.findAll().stream().map(e -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("enrollmentId", e.getEnrollmentId());
                row.put("youthId", e.getYouth().getUserId());
                row.put("youthName", e.getYouth().getName());
                row.put("activityId", e.getActivity().getActivityId());
                row.put("activityTitle", e.getActivity().getTitle());
                row.put("status", e.getStatus());
                row.put("enrollmentDate", e.getEnrollmentDate());
                row.put("completedAt", e.getCompletedAt());
                return row;
            }).toList();
            case "certificates" -> certificateRepository.findAll().stream().map(c -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("certificateId", c.getCertificateId());
                row.put("certificateNumber", c.getCertificateNumber());
                row.put("youthId", c.getYouth().getUserId());
                row.put("youthName", c.getYouth().getName());
                row.put("achievement", c.getAchievement());
                row.put("issueDate", c.getIssueDate());
                return row;
            }).toList();
            case "projects" -> projectRepository.findAll().stream().map(p -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("projectId", p.getProjectId());
                row.put("title", p.getTitle());
                row.put("challengeArea", p.getChallengeArea());
                row.put("status", p.getStatus());
                row.put("teamId", p.getTeam().getTeamId());
                row.put("teamName", p.getTeam().getTeamName());
                row.put("submittedAt", p.getSubmittedAt());
                return row;
            }).toList();
            case "opportunities" -> opportunityRepository.findAll().stream().map(o -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("opportunityId", o.getOpportunityId());
                row.put("title", o.getTitle());
                row.put("type", o.getOpportunityType());
                row.put("partner", o.getPartner().getOrganisationName());
                row.put("deadline", o.getDeadline());
                row.put("status", o.getStatus());
                return row;
            }).toList();
            default -> throw new com.yep.exception.BadRequestException("INVALID_REPORT",
                    "Unknown report type. Valid: participants, enrollments, certificates, projects, opportunities.");
        };

        if ("csv".equalsIgnoreCase(format) && !rows.isEmpty()) {
            writeCsv(rows, response);
            return null;
        }
        return rows;
    }

    /** Serialises a list of maps as RFC-4180 CSV to the HTTP response output stream. */
    private void writeCsv(List<Map<String, Object>> rows, HttpServletResponse response) {
        try {
            java.io.PrintWriter writer = response.getWriter();
            writer.println(String.join(",", rows.get(0).keySet()));
            for (Map<String, Object> row : rows) {
                writer.println(row.values().stream()
                        .map(v -> v == null ? "" : "\"" + v.toString().replace("\"", "\"\"") + "\"")
                        .collect(java.util.stream.Collectors.joining(",")));
            }
            writer.flush();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error writing CSV", e);
        }
    }
}
