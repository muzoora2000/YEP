package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.certificate.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByVerificationCode(String verificationCode);

    boolean existsByVerificationCode(String verificationCode);

    List<Certificate> findByRecipientId(Long recipientId);

    /**
     * Written as an explicit query on purpose: the derived query parser would read
     * the trailing {@code OrProject} of {@code relatedActivityOrProject} as an
     * {@code OR} operator.
     */
    @Query("SELECT COUNT(c) > 0 FROM Certificate c "
            + "WHERE c.recipient.id = :recipientId AND c.relatedActivityOrProject = :reference")
    boolean existsForRecipientAndReference(@Param("recipientId") Long recipientId,
            @Param("reference") String reference);
}
