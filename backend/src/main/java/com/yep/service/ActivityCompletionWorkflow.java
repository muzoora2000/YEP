package com.yep.service;

import java.util.List;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.ActivityCompletionSummary;
import com.yep.repository.EnrollmentRepository;

/**
 * Template Method: the completion algorithm is fixed (mark the activity COMPLETED,
 * walk the ENROLLED roster and issue certificates to everyone who qualifies) while
 * subclasses vary the eligibility rule and the achievement text per activity level.
 */
public abstract class ActivityCompletionWorkflow {

    private final ActivityService activityService;
    private final CertificateService certificateService;
    private final EnrollmentRepository enrollmentRepository;

    protected ActivityCompletionWorkflow(ActivityService activityService, CertificateService certificateService,
            EnrollmentRepository enrollmentRepository) {
        this.activityService = activityService;
        this.certificateService = certificateService;
        this.enrollmentRepository = enrollmentRepository;
    }

    public final ActivityCompletionSummary complete(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        activityService.markCompleted(activity);
        List<Enrollment> enrolled = enrollmentRepository.findByActivityActivityIdAndStatus(activityId,
                EnrollmentStatus.ENROLLED);
        int issued = 0;
        for (Enrollment enrollment : enrolled) {
            YouthParticipant youth = enrollment.getYouth();
            if (isEligible(youth, activity)) {
                certificateService.issueCertificate(youth, activity, achievementText(youth, activity));
                issued++;
            }
        }
        return new ActivityCompletionSummary(issued, issued);
    }

    protected abstract boolean isEligible(YouthParticipant participant, LearningActivity activity);

    protected abstract String achievementText(YouthParticipant participant, LearningActivity activity);
}
