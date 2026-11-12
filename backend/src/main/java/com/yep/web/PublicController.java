package com.yep.web;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.PageResponse;
import com.yep.dto.PartnerDtos.OpportunityResponse;
import com.yep.dto.PartnerDtos.PublicSettingsResponse;
import com.yep.dto.ProjectDtos.ProjectResponse;
import com.yep.service.AdminPlatformService;
import com.yep.service.PartnerService;
import com.yep.service.ProjectService;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final AdminPlatformService adminPlatformService;
    private final PartnerService partnerService;
    private final ProjectService projectService;

    public PublicController(AdminPlatformService adminPlatformService, PartnerService partnerService,
            ProjectService projectService) {
        this.adminPlatformService = adminPlatformService;
        this.partnerService = partnerService;
        this.projectService = projectService;
    }

    @GetMapping("/settings")
    public PublicSettingsResponse settings() {
        return adminPlatformService.publicSettings();
    }

    @GetMapping("/opportunities")
    public PageResponse<OpportunityResponse> opportunities(
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(partnerService.publicOpportunities(pageable));
    }

    @GetMapping("/projects/showcase")
    public List<ProjectResponse> showcase() {
        return projectService.showcase();
    }
}
