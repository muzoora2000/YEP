package com.yep.domain.interfaces;

import java.util.List;

import com.yep.domain.user.Participant;

/**
 * Anything that can be turned into a certificate once it is finished.
 * Implemented by {@link Participant} (level one completion) and by
 * {@code ImpactProject} (level two completion) so one certificate service can
 * issue documents for both cases without branching on concrete types.
 */
public interface Certifiable {

    /** Title printed on the certificate. */
    String getCertificateTitle();

    /** Human readable reference (activity name, project name, ...). */
    String getCertificateReference();

    /** Whether the object has reached the state required for certification. */
    boolean qualifiesForCertificate();

    /** Participants that receive the generated certificate(s). */
    List<Participant> getCertificateRecipients();
}
