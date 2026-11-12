package com.yep.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.TaskType;
import com.yep.domain.learning.Assessment;
import com.yep.domain.learning.AssessmentTask;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.AssessmentResponse;
import com.yep.dto.LearningDtos.GradeRequest;
import com.yep.dto.LearningDtos.SubmissionRequest;
import com.yep.dto.LearningDtos.TaskRequest;
import com.yep.dto.LearningDtos.TaskResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.AssessmentTaskRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AssessmentService(AssessmentRepository assessmentRepository, AssessmentTaskRepository taskRepository,
            UserRepository userRepository, ActivityService activityService, SecurityUtils securityUtils,
            AuditService auditService, NotificationService notificationService) {
        this.assessmentRepository = assessmentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public TaskResponse createTask(Long activityId, TaskRequest request) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        AssessmentTask task = new AssessmentTask(activity, request.title().trim(), request.taskType());
        applyTask(task, request);
        task = taskRepository.save(task);
        auditService.record(currentUser(), "TASK_CREATED", "AssessmentTask", task.getTaskId(), task.getTitle());
        return toTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        AssessmentTask task = findTask(taskId);
        activityService.requireManage(task.getActivity());
        List<Assessment> submissions = assessmentRepository.findByTaskTaskId(taskId);
        if (request.maxScore() != null && submissions.stream()
                .anyMatch(submission -> submission.getScore() != null && submission.getScore() > request.maxScore())) {
            throw new BusinessRuleException("MAX_SCORE_TOO_LOW",
                    "Maximum score cannot be lower than an existing submission score.");
        }
        task.setTitle(request.title().trim());
        task.setTaskType(request.taskType());
        applyTask(task, request);
        taskRepository.save(task);
        auditService.record(currentUser(), "TASK_UPDATED", "AssessmentTask", taskId, task.getTitle());
        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        User actor = currentUser();
        boolean manager = isManager(activity, actor);
        if (!manager) {
            activityService.requireEnrolled(activity, actor.getUserId());
            return taskRepository.findByActivityActivityIdAndPublishedTrueOrderByDueDateAsc(activityId).stream()
                    .map(this::toTaskResponse).toList();
        }
        return taskRepository.findByActivityActivityIdOrderByDueDateAsc(activityId).stream()
                .map(this::toTaskResponse).toList();
    }

    @Transactional
    public AssessmentResponse submit(Long taskId, SubmissionRequest request) {
        AssessmentTask task = findTask(taskId);
        if (!task.isPublished()) {
            throw new BusinessRuleException("TASK_NOT_PUBLISHED", "This task is not available.");
        }
        YouthParticipant youth = currentParticipant();
        activityService.requireEnrolled(task.getActivity(), youth.getUserId());
        boolean late = task.getDueDate() != null && OffsetDateTime.now().isAfter(task.getDueDate());
        if (late && task.getTaskType() == TaskType.QUIZ) {
            throw new BusinessRuleException("DEADLINE_PASSED", "The deadline for this quiz has passed.");
        }
        if (assessmentRepository.existsByTaskTaskIdAndYouthUserId(taskId, youth.getUserId())) {
            throw new ConflictException("ALREADY_SUBMITTED", "You have already submitted this task.");
        }
        if (task.getTaskType() == TaskType.QUIZ && (request.answers() == null || request.answers().isEmpty())) {
            throw new BadRequestException("ANSWERS_REQUIRED", "Quiz answers are required.");
        }
        if (task.getTaskType() == TaskType.ASSIGNMENT
                && (request.submissionText() == null || request.submissionText().isBlank())) {
            throw new BadRequestException("SUBMISSION_REQUIRED", "A written submission is required.");
        }
        Assessment assessment = new Assessment(task.getActivity(), youth, task);
        assessment.setAnswers(request.answers());
        assessment.setSubmissionText(request.submissionText());
        assessment.setMaxScore(task.getMaxScore());
        // Quizzes are auto-graded on submission; late assignments are accepted and flagged.
        if (task.getTaskType() == TaskType.QUIZ) {
            autoGradeQuiz(task, assessment);
        }
        assessment = assessmentRepository.save(assessment);
        auditService.record(youth, "ASSESSMENT_SUBMITTED", "Assessment", assessment.getAssessmentId(), task.getTitle());
        return toResponse(assessment);
    }

    private void autoGradeQuiz(AssessmentTask task, Assessment assessment) {
        List<Map<String, Object>> questions = task.getQuestions();
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<Integer> answers = assessment.getAnswers() == null ? List.of() : assessment.getAnswers();
        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> question = questions.get(i);
            Object correctIndex = question == null ? null : question.get("correctIndex");
            if (correctIndex == null || i >= answers.size() || answers.get(i) == null) {
                continue;
            }
            if (((Number) correctIndex).intValue() == answers.get(i)) {
                correct++;
            }
        }
        int score = (int) Math.round(correct * task.getMaxScore() / (double) questions.size());
        assessment.evaluate(score, "Auto-graded: " + correct + " of " + questions.size() + " questions correct.");
    }

    @Transactional
    public AssessmentResponse grade(Long assessmentId, GradeRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("ASSESSMENT_NOT_FOUND", "Submission not found."));
        activityService.requireManage(assessment.getActivity());
        int max = assessment.getMaxScore() == null ? 100 : assessment.getMaxScore();
        if (request.score() > max) {
            throw new BadRequestException("SCORE_TOO_HIGH", "Score cannot exceed " + max + ".");
        }
        assessment.evaluate(request.score(), trimToNull(request.feedback()));
        assessment.setGradedBy(currentUser());
        assessmentRepository.save(assessment);
        notificationService.notify(assessment.getYouth(),
                "Your submission for \"" + assessment.getTask().getTitle() + "\" was graded: " + request.score()
                        + "/" + max + ".",
                "/my/results", NotificationType.IN_APP);
        auditService.record(currentUser(), "ASSESSMENT_GRADED", "Assessment", assessmentId,
                "Score " + request.score() + "/" + max);
        return toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> submissionsForTask(Long taskId) {
        AssessmentTask task = findTask(taskId);
        activityService.requireManage(task.getActivity());
        return assessmentRepository.findByTaskTaskId(taskId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> mySubmissions() {
        return assessmentRepository.findByYouthUserId(securityUtils.currentUserId()).stream()
                .map(this::toResponse).toList();
    }

    public AssessmentTask findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Assessment task not found."));
    }

    public TaskResponse toTaskResponse(AssessmentTask task) {
        return new TaskResponse(task.getTaskId(), task.getActivity().getActivityId(), task.getTitle(),
                task.getTaskType(), task.getInstructions(), task.getQuestions(), task.getMaxScore(),
                task.getDueDate(), task.isPublished());
    }

    public AssessmentResponse toResponse(Assessment assessment) {
        AssessmentTask task = assessment.getTask();
        boolean late = task != null && task.getDueDate() != null && assessment.getAssessmentDate() != null
                && assessment.getAssessmentDate().isAfter(task.getDueDate());
        return new AssessmentResponse(assessment.getAssessmentId(),
                task == null ? null : task.getTaskId(),
                task == null ? null : task.getTitle(),
                assessment.getActivity().getActivityId(), assessment.getYouth().getUserId(),
                assessment.getYouth().getName(), assessment.getScore(), assessment.getMaxScore(),
                assessment.percentage(), assessment.getStatus(), assessment.getFeedback(), assessment.getAnswers(),
                assessment.getSubmissionText(), assessment.getAssessmentDate(),
                assessment.getGradedBy() == null ? null : assessment.getGradedBy().getName(), late);
    }

    private void applyTask(AssessmentTask task, TaskRequest request) {
        task.setInstructions(trimToNull(request.instructions()));
        task.setQuestions(request.questions());
        task.setMaxScore(request.maxScore() == null ? 100 : request.maxScore());
        task.setDueDate(request.dueDate());
        task.setPublished(request.published() == null || request.published());
        if (task.getMaxScore() < 1) {
            throw new BadRequestException("INVALID_MAX_SCORE", "Maximum score must be at least 1.");
        }
    }

    private boolean isManager(LearningActivity activity, User actor) {
        return actor.getRole() == Role.ADMIN
                || (actor.getRole() == Role.FACILITATOR && activity.getFacilitator().getUserId().equals(actor.getUserId()));
    }

    private YouthParticipant currentParticipant() {
        User user = currentUser();
        if (!(user instanceof YouthParticipant participant)) {
            throw new ForbiddenException("PARTICIPANT_ONLY", "Only youth participants submit assessments.");
        }
        return participant;
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
