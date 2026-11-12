package com.yep.service;

import com.yep.domain.learning.Certificate;
import com.yep.domain.user.YouthParticipant;
import com.yep.domain.activity.LearningActivity;

/** Interface abstraction for certificate issuance. CertificateService implements this. */
public interface CertificateIssuer {
    Certificate issueCertificate(YouthParticipant participant, LearningActivity activity, String achievement);
    boolean isEligible(YouthParticipant participant, LearningActivity activity);
}
