package com.yep.service;

import org.springframework.stereotype.Service;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.user.YouthParticipant;
import com.yep.repository.EnrollmentRepository;

/**
 * Level Two completion: eligibility additionally requires the team project to be
 * APPROVED or COMPLETED, and the achievement text names that project.
 */
@Service
public class LevelTwoCompletionWorkflow extends ActivityCompletionWorkflow {

    private final CertificateService certificateService;

    public LevelTwoCompletionWorkflow(ActivityService activityService, CertificateService certificateService,
            EnrollmentRepository enrollmentRepository) {
        super(activityService, certificateService, enrollmentRepository);
        this.certificateService = certificateService;
    }

    @Override
    protected boolean isEligible(YouthParticipant participant, LearningActivity activity) {
        return certificateService.isEligible(participant, activity)
                && certificateService.isProjectComplete(certificateService.projectFor(participant, activity));
    }

    @Override
    protected String achievementText(YouthParticipant participant, LearningActivity activity) {
        return certificateService.achievementText(activity, certificateService.projectFor(participant, activity));
    }
}
