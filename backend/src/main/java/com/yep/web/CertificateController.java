package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.CertificateResponse;
import com.yep.service.CertificateService;

@RestController
@RequestMapping("/api/v1")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/me/certificates")
    public List<CertificateResponse> myCertificates() {
        return certificateService.myCertificates();
    }

    @GetMapping("/certificates/me")
    public List<CertificateResponse> myCertificatesAlias() {
        return certificateService.myCertificates();
    }

    @GetMapping(value = "/certificates/{certificateId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfById(@PathVariable Long certificateId) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"yep-certificate-" + certificateId + ".pdf\"")
                .body(certificateService.pdfById(certificateId));
    }

    @PostMapping("/activities/{activityId}/certificates/{youthId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<CertificateResponse> issue(@PathVariable Long activityId, @PathVariable Long youthId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.issue(activityId, youthId));
    }

    @PutMapping("/admin/certificates/{certificateId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public CertificateResponse revoke(@PathVariable Long certificateId) {
        return certificateService.revoke(certificateId);
    }

    @GetMapping(value = "/public/certificates/{certificateNumber}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String certificateNumber) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"yep-certificate-" + certificateNumber + ".pdf\"")
                .body(certificateService.pdf(certificateNumber));
    }

    @GetMapping("/public/certificates/{certificateNumber}")
    public CertificateResponse verify(@PathVariable String certificateNumber) {
        return certificateService.verify(certificateNumber);
    }

    // Spec-compatible alias for the public verification endpoint.
    @GetMapping("/certificates/verify/{code}")
    public CertificateResponse verifyAlias(@PathVariable String code) {
        return certificateService.verify(code);
    }
}
