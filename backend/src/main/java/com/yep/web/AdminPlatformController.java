package com.yep.web;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.ModerationQueueResponse;
import com.yep.dto.LearningDtos.ModerationRequest;
import com.yep.dto.LearningDtos.PostResponse;
import com.yep.dto.PageResponse;
import com.yep.dto.PartnerDtos.AuditLogResponse;
import com.yep.dto.PartnerDtos.DashboardResponse;
import com.yep.dto.PartnerDtos.PartnerSummary;
import com.yep.dto.PartnerDtos.PartnerVerificationRequest;
import com.yep.dto.PartnerDtos.SettingResponse;
import com.yep.dto.PartnerDtos.SettingUpdateRequest;
import com.yep.service.AdminPlatformService;
import com.yep.service.DiscussionService;
import com.yep.service.PartnerService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlatformController {

    private final AdminPlatformService adminPlatformService;
    private final PartnerService partnerService;
    private final DiscussionService discussionService;

    public AdminPlatformController(AdminPlatformService adminPlatformService, PartnerService partnerService,
            DiscussionService discussionService) {
        this.adminPlatformService = adminPlatformService;
        this.partnerService = partnerService;
        this.discussionService = discussionService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminPlatformService.dashboard();
    }

    @GetMapping("/moderation")
    public List<ModerationQueueResponse> moderationQueue() {
        return discussionService.moderationQueue();
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> auditLogs(
            @PageableDefault(size = 30, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(adminPlatformService.auditLogs(pageable));
    }

    @GetMapping("/settings")
    public List<SettingResponse> settings() {
        return adminPlatformService.settings();
    }

    @PutMapping("/settings/{key}")
    public SettingResponse updateSetting(@PathVariable String key,
            @Valid @RequestBody SettingUpdateRequest request) {
        return adminPlatformService.updateSetting(key, request.value());
    }

    @GetMapping("/partners")
    public PageResponse<PartnerSummary> partners(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(partnerService.listPartners(pageable));
    }

    @PutMapping("/partners/{partnerId}/verification")
    public PartnerSummary verifyPartner(@PathVariable Long partnerId,
            @RequestBody PartnerVerificationRequest request) {
        return partnerService.verifyPartner(partnerId, request.verified());
    }

    /**
     * Admin shortcut to hide a discussion post. Delegates to DiscussionService.moderate()
     * — Polymorphism: same service method, different security context.
     */
    @PutMapping("/posts/{postId}/hide")
    public PostResponse hidePost(@PathVariable Long postId,
            @RequestBody ModerationRequest request) {
        return discussionService.moderate(postId, request);
    }

    /**
     * Generate a CSV or JSON summary report. Strategy: report type drives
     * which repository query runs. CSV export follows text/csv content negotiation.
     */
    @GetMapping(value = "/reports/{type}", produces = { MediaType.APPLICATION_JSON_VALUE, "text/csv" })
    public Object report(@PathVariable String type,
            @RequestParam(defaultValue = "json") String format,
            HttpServletResponse response) {
        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"report-" + type + ".csv\"");
        }
        return adminPlatformService.report(type, format, response);
    }
}
