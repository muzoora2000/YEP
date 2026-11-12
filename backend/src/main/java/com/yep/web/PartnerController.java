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

import com.yep.dto.PartnerDtos.InterestResponse;
import com.yep.dto.PartnerDtos.OpportunityRequest;
import com.yep.dto.PartnerDtos.OpportunityResponse;
import com.yep.dto.PartnerDtos.SupportResponse;
import com.yep.service.PartnerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/opportunities")
    public List<OpportunityResponse> myOpportunities() {
        return partnerService.myOpportunities();
    }

    @PostMapping("/opportunities")
    public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.createOpportunity(request));
    }

    @PutMapping("/opportunities/{opportunityId}")
    public OpportunityResponse update(@PathVariable Long opportunityId,
            @Valid @RequestBody OpportunityRequest request) {
        return partnerService.updateOpportunity(opportunityId, request);
    }

    @PutMapping("/opportunities/{opportunityId}/close")
    public OpportunityResponse close(@PathVariable Long opportunityId) {
        return partnerService.closeOpportunity(opportunityId);
    }

    @GetMapping("/opportunities/{opportunityId}/interests")
    public List<InterestResponse> interests(@PathVariable Long opportunityId) {
        return partnerService.opportunityInterests(opportunityId);
    }

    @GetMapping("/supports")
    public List<SupportResponse> supports() {
        return partnerService.mySupports();
    }
}
