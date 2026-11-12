package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.PartnerDtos.InterestRequest;
import com.yep.dto.PartnerDtos.InterestResponse;
import com.yep.service.PartnerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final PartnerService partnerService;

    public OpportunityController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/my-interests")
    public List<InterestResponse> myInterests() {
        return partnerService.myInterests();
    }

    @PostMapping("/{opportunityId}/interest")
    public ResponseEntity<InterestResponse> expressInterest(@PathVariable Long opportunityId,
            @Valid @RequestBody InterestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerService.expressInterest(opportunityId, request));
    }
}
