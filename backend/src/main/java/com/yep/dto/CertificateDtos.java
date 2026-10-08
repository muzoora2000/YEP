package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.certificate.Certificate;

import jakarta.validation.constraints.NotNull;

/** Contracts for {@code /api/v1/certificates}. */
public final class CertificateDtos {

    private CertificateDtos() {
    }

    public record CertificateResponse(
            Long id,
            String title,
            Long recipientId,
            String recipientName,
            OffsetDateTime issueDate,
            String verificationCode,
            String relatedActivityOrProject) {

        public static CertificateResponse of(Certificate certificate) {
            return new CertificateResponse(
                    certificate.getId(),
                    certificate.getTitle(),
                    certificate.getRecipient() != null ? certificate.getRecipient().getId() : null,
                    certificate.getRecipient() != null ? certificate.getRecipient().getFullName() : null,
                    certificate.getIssueDate(),
                    certificate.getVerificationCode(),
                    certificate.getRelatedActivityOrProject());
        }
    }

    /**
     * Either {@code recipientId} (manual issue to one participant) or
     * {@code projectId} (issue to every member of a completed project) must be set.
     */
    public record IssueCertificateRequest(
            Long recipientId,
            Long projectId,
            String title,
            String relatedActivityOrProject) {
    }

    /** Public payload of {@code GET /certificates/verify/{code}} - deliberately minimal PII. */
    public record VerificationResponse(
            boolean valid,
            String message,
            String title,
            String recipientName,
            OffsetDateTime issueDate,
            String relatedActivityOrProject) {

        public static VerificationResponse found(Certificate certificate) {
            return new VerificationResponse(
                    true,
                    "Certificate verified",
                    certificate.getTitle(),
                    certificate.getRecipient() != null ? certificate.getRecipient().getFullName() : null,
                    certificate.getIssueDate(),
                    certificate.getRelatedActivityOrProject());
        }

        public static VerificationResponse notFound() {
            return new VerificationResponse(false, "No certificate matches this verification code", null, null, null, null);
        }
    }

    public record RecipientIdsRequest(@NotNull Long recipientId) {
    }
}
