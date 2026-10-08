package com.yep.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.CompletionStatus;
import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.exchange.Enrollment;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.user.Participant;
import com.yep.dto.EnrollmentDtos;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ParticipantRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;
import com.yep.service.policy.EnrollmentPolicy;
import com.yep.service.policy.LevelOneEnrollmentPolicy;
import com.yep.service.policy.LevelTwoEnrollmentPolicy;

/**
 * Enrollment into virtual exchanges.
 *
 * <p>The level specific rules live in {@link EnrollmentPolicy} implementations
 * that are looked up by {@link ExchangeLevel} - adding a third exchange level
 * would only require a new policy bean, no change here (Open/Closed principle).</p>
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ExchangeService exchangeService;
    private final ParticipantRepository participantRepository;
    private final CertificateService certificateService;
    private final Map<ExchangeLevel, EnrollmentPolicy> policies = new EnumMap<>(ExchangeLevel.class);

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            ExchangeService exchangeService,
            ParticipantRepository participantRepository,
            CertificateService certificateService,
            LevelOneEnrollmentPolicy levelOnePolicy,
            LevelTwoEnrollmentPolicy levelTwoPolicy) {
        this.enrollmentRepository = enrollmentRepository;
        this.exchangeService = exchangeService;
        this.participantRepository = participantRepository;
        this.certificateService = certificateService;
        this.policies.put(ExchangeLevel.LEVEL_ONE, levelOnePolicy);
        this.policies.put(ExchangeLevel.LEVEL_TWO, levelTwoPolicy);
    }

    @Transactional
    public EnrollmentDtos.EnrollmentResponse enroll(Long exchangeId) {
        AuthenticatedUser principal = SecurityUtils.current();
        Participant participant = participantRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Only participants can enroll in activities"));
        VirtualExchange exchange = exchangeService.getEntity(exchangeId);

        if (enrollmentRepository.existsByParticipantIdAndVirtualExchangeId(participant.getId(), exchangeId)) {
            throw new ConflictException("You are already enrolled in this activity");
        }

        EnrollmentPolicy policy = policies.get(exchange.getLevel());
        policy.check(exchange, participant);

        return EnrollmentDtos.EnrollmentResponse.of(enrollmentRepository.save(Enrollment.open(participant, exchange)));
    }

    @Transactional
    public EnrollmentDtos.EnrollmentResponse withdraw(Long enrollmentId) {
        Enrollment enrollment = find(enrollmentId);
        AuthenticatedUser principal = SecurityUtils.current();
        if (enrollment.getParticipant() == null || !enrollment.getParticipant().getId().equals(principal.id())) {
            if (!principal.isAdmin() && !principal.isStaff()) {
                throw new ForbiddenException("You can only withdraw your own enrollment");
            }
        }
        enrollment.markDropped();
        return EnrollmentDtos.EnrollmentResponse.of(enrollmentRepository.save(enrollment));
    }

    /** Facilitator marks a completed session - triggers certificate issuance through {@link Certifiable}. */
    @Transactional
    public EnrollmentDtos.EnrollmentResponse complete(Long enrollmentId) {
        Enrollment enrollment = find(enrollmentId);
        if (enrollment.getCompletionStatus() == CompletionStatus.COMPLETED) {
            return EnrollmentDtos.EnrollmentResponse.of(enrollment);
        }
        enrollment.markCompleted();
        Enrollment saved = enrollmentRepository.save(enrollment);

        Participant participant = saved.getParticipant();
        if (participant != null && participant.qualifiesForCertificate()) {
            certificateService.issueFor(participant, saved.getVirtualExchange() != null
                    ? saved.getVirtualExchange().getTitle()
                    : participant.getCertificateReference());
        }
        return EnrollmentDtos.EnrollmentResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDtos.EnrollmentResponse> mine() {
        AuthenticatedUser principal = SecurityUtils.current();
        return enrollmentRepository.findByParticipantId(principal.id()).stream()
                .map(EnrollmentDtos.EnrollmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDtos.EnrollmentResponse> byExchange(Long exchangeId) {
        return enrollmentRepository.findByVirtualExchangeId(exchangeId).stream()
                .map(EnrollmentDtos.EnrollmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countForExchange(Long exchangeId) {
        return enrollmentRepository.countByVirtualExchangeId(exchangeId);
    }

    private Enrollment find(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.resource("Enrollment", id));
    }
}
