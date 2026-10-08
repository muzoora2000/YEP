package com.yep.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.certificate.Certificate;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.user.Participant;
import com.yep.dto.CertificateDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.CertificateRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.ParticipantRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * Issues and verifies certificates.
 *
 * <p>{@link #issueFor(Certifiable, String)} is the polymorphic entry point: a
 * completed {@link ImpactProject} yields one certificate per team member and a
 * completed participant enrollment yields exactly one, without the service
 * ever down-casting.</p>
 */
@Service
public class CertificateService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final ImpactProjectRepository projectRepository;
    private final ParticipantRepository participantRepository;

    public CertificateService(
            CertificateRepository certificateRepository,
            ImpactProjectRepository projectRepository,
            ParticipantRepository participantRepository) {
        this.certificateRepository = certificateRepository;
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * Issues a certificate for every recipient of the given source object.
     *
     * @return the certificates that were actually created (already issued ones are skipped)
     */
    @Transactional
    public List<CertificateDtos.CertificateResponse> issueFor(Certifiable source, String reference) {
        if (source == null || !source.qualifiesForCertificate()) {
            return List.of();
        }
        String relatedReference = reference != null ? reference : source.getCertificateReference();
        String title = source.getCertificateTitle();

        List<CertificateDtos.CertificateResponse> issued = new ArrayList<>();
        for (Participant recipient : source.getCertificateRecipients()) {
            if (certificateRepository.existsForRecipientAndReference(recipient.getId(),
                    relatedReference)) {
                continue;
            }
            Certificate certificate = Certificate.issuedTo(recipient, title, relatedReference, newCode());
            issued.add(CertificateDtos.CertificateResponse.of(certificateRepository.save(certificate)));
        }
        return issued;
    }

    @Transactional
    public List<CertificateDtos.CertificateResponse> issueForProject(Long projectId) {
        ImpactProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> NotFoundException.resource("Project", projectId));
        if (project.getTeam() == null) {
            throw new BadRequestException("The project has no team attached");
        }
        return issueFor(project, project.getTitle());
    }

    @Transactional
    public CertificateDtos.CertificateResponse issueToParticipant(Long recipientId, String title, String reference) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.isStaff()) {
            throw new ForbiddenException("Only facilitators, mentors or administrators can issue certificates");
        }
        Participant recipient = participantRepository.findById(recipientId)
                .orElseThrow(() -> NotFoundException.resource("Participant", recipientId));

        if (certificateRepository.existsForRecipientAndReference(recipientId, reference)) {
            throw new com.yep.exception.ConflictException("A certificate for this activity already exists for this participant");
        }
        String finalTitle = title != null && !title.isBlank() ? title : "Certificate of Participation - "
                + recipient.getFullName();
        Certificate certificate = Certificate.issuedTo(recipient, finalTitle,
                reference != null ? reference : "YEP Virtual Exchange Participation", newCode());
        return CertificateDtos.CertificateResponse.of(certificateRepository.save(certificate));
    }

    @Transactional(readOnly = true)
    public CertificateDtos.VerificationResponse verify(String code) {
        if (code == null || code.isBlank()) {
            return CertificateDtos.VerificationResponse.notFound();
        }
        Optional<Certificate> certificate = certificateRepository.findByVerificationCode(code.trim());
        return certificate.map(CertificateDtos.VerificationResponse::found)
                .orElseGet(CertificateDtos.VerificationResponse::notFound);
    }

    @Transactional(readOnly = true)
    public List<CertificateDtos.CertificateResponse> mine() {
        AuthenticatedUser principal = SecurityUtils.current();
        return certificateRepository.findByRecipientId(principal.id()).stream()
                .map(CertificateDtos.CertificateResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateDtos.CertificateResponse> all() {
        return certificateRepository.findAll().stream()
                .map(CertificateDtos.CertificateResponse::of)
                .toList();
    }

    /** Public, non-guessable verification handle (no sequential ids leaked). */
    private String newCode() {
        String code;
        do {
            byte[] buffer = new byte[9];
            RANDOM.nextBytes(buffer);
            code = "YEP-" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
        } while (certificateRepository.existsByVerificationCode(code));
        return code;
    }
}
