package com.yep.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.CertificateDtos;
import com.yep.service.CertificateService;

import jakarta.validation.Valid;

/**
 * Certificates - {@code /api/v1/certificates}.
 * {@code GET /certificates/verify/{code}} is intentionally public so employers
 * and universities can verify a credential with only its code.
 */
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/verify/{code}")
    public CertificateDtos.VerificationResponse verify(@PathVariable String code) {
        return certificateService.verify(code);
    }

    @GetMapping("/mine")
    public List<CertificateDtos.CertificateResponse> mine() {
        return certificateService.mine();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public List<CertificateDtos.CertificateResponse> all() {
        return certificateService.all();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public ResponseEntity<CertificateDtos.CertificateResponse> issue(
            @Valid @RequestBody CertificateDtos.IssueCertificateRequest request) {
        if (request.projectId() != null) {
            List<CertificateDtos.CertificateResponse> issued = certificateService.issueForProject(request.projectId());
            if (issued.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(issued.get(0));
        }
        if (request.recipientId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.issueToParticipant(request.recipientId(), request.title(),
                        request.relatedActivityOrProject()));
    }
}
