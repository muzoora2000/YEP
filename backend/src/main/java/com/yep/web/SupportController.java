package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.PartnerDtos.SupportDecisionRequest;
import com.yep.dto.PartnerDtos.SupportRequest;
import com.yep.dto.PartnerDtos.SupportResponse;
import com.yep.service.PartnerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class SupportController {

    private final PartnerService partnerService;

    public SupportController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/projects/{projectId}/supports")
    public List<SupportResponse> projectSupports(@PathVariable Long projectId) {
        return partnerService.projectSupports(projectId);
    }

    @PostMapping("/projects/{projectId}/supports")
    public ResponseEntity<SupportResponse> offerSupport(@PathVariable Long projectId,
            @Valid @RequestBody SupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerService.offerSupport(projectId, request));
    }

    @PutMapping("/supports/{supportId}/respond")
    public SupportResponse respond(@PathVariable Long supportId,
            @RequestBody SupportDecisionRequest request) {
        return partnerService.respondToSupport(supportId, request.accept());
    }
}
