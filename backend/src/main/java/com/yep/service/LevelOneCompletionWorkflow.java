package com.yep.service;

import org.springframework.stereotype.Service;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.user.YouthParticipant;
import com.yep.repository.EnrollmentRepository;

/** Level One completion: issue certificates to enrolled participants who met the requirements. */
@Service
public class LevelOneCompletionWorkflow extends ActivityCompletionWorkflow {

    private final CertificateService certificateService;

    public LevelOneCompletionWorkflow(ActivityService activityService, CertificateService certificateService,
            EnrollmentRepository enrollmentRepository) {
        super(activityService, certificateService, enrollmentRepository);
        this.certificateService = certificateService;
    }

    @Override
    protected boolean isEligible(YouthParticipant participant, LearningActivity activity) {
        return certificateService.isEligible(participant, activity);
    }

    @Override
    protected String achievementText(YouthParticipant participant, LearningActivity activity) {
        return certificateService.achievementText(activity, null);
    }
}
