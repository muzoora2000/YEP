package com.yep.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.enums.AssessmentStatus;
import com.yep.domain.enums.AttendanceStatus;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.MaterialType;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.enums.TaskType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LearningDtos {

    private LearningDtos() {
    }

    public record CreateActivityRequest(
            @NotNull ActivityLevel level,
            @NotBlank @Size(max = 190) String title,
            @Size(max = 4000) String description,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @Size(max = 80) String category,
            @Min(1) Integer capacity,
            Long partnerId,
            Long facilitatorId,
            @Min(0) @Max(100) Integer passMark,
            @Min(0) @Max(100) Integer minAttendancePercent,
            @Min(1) Integer minTeamSize,
            @Min(1) Integer maxTeamSize) {
    }

    public record UpdateActivityRequest(
            @Size(max = 190) String title,
            @Size(max = 4000) String description,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 80) String category,
            @Min(1) Integer capacity,
            Long partnerId,
            @Min(0) @Max(100) Integer passMark,
            @Min(0) @Max(100) Integer minAttendancePercent,
            @Min(1) Integer minTeamSize,
            @Min(1) Integer maxTeamSize) {
    }

    public record ActivityResponse(
            Long activityId,
            ActivityLevel level,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            Integer capacity,
            ActivityStatus status,
            int passMark,
            int minAttendancePercent,
            int minTeamSize,
            int maxTeamSize,
            Long facilitatorId,
            String facilitatorName,
            Long partnerId,
            String partnerName,
            long enrolledCount,
            long waitlistedCount,
            List<String> levelFeatures) {
    }

    public record EnrollmentResponse(
            Long enrollmentId,
            Long activityId,
            String activityTitle,
            Long youthId,
            String youthName,
            String youthEmail,
            EnrollmentStatus status,
            OffsetDateTime enrollmentDate,
            OffsetDateTime completedAt) {
    }

    public record EnrollmentStatusRequest(@NotNull EnrollmentStatus status) {
    }

    /** Facilitator decision on a Level Two application: SELECT (alias ENROLL/ACCEPT) or REJECT. */
    public record EnrollmentDecisionRequest(@NotBlank @Size(max = 20) String decision) {
    }

    public record SessionRequest(
            @NotBlank @Size(max = 190) String title,
            @NotNull OffsetDateTime scheduledStart,
            @Min(1) int durationMinutes,
            @NotBlank @Size(max = 1000) String meetingLink) {
    }

    public record SessionResponse(
            Long sessionId,
            Long activityId,
            String title,
            OffsetDateTime scheduledStart,
            int durationMinutes,
            String meetingLink,
            SessionStatus status,
            boolean checkInOpen) {
    }

    public record AttendanceEntry(Long youthId, @NotNull AttendanceStatus status) {
    }

    public record AttendanceBulkRequest(@NotEmpty List<AttendanceEntry> entries) {
    }

    public record AttendanceResponse(
            Long attendanceId,
            Long sessionId,
            Long youthId,
            String youthName,
            AttendanceStatus status,
            OffsetDateTime markedAt) {
    }

    public record MaterialRequest(
            @NotBlank @Size(max = 190) String title,
            @NotNull MaterialType materialType,
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 2000) String description,
            Boolean published) {
    }

    public record MaterialResponse(
            Long materialId,
            Long activityId,
            String title,
            MaterialType materialType,
            String url,
            String description,
            String uploadedByName,
            OffsetDateTime uploadedAt,
            boolean published) {
    }

    public record ThreadRequest(@NotBlank @Size(max = 190) String topic, Long teamId) {
    }

    public record ThreadResponse(
            Long threadId,
            Long activityId,
            Long teamId,
            String topic,
            OffsetDateTime createdAt,
            long postCount) {
    }

    public record PostRequest(@NotBlank @Size(max = 4000) String content) {
    }

    public record PostResponse(
            Long postId,
            Long threadId,
            Long authorId,
            String authorName,
            String content,
            OffsetDateTime postedAt,
            boolean flagged,
            String flagReason,
            boolean hidden) {
    }

    public record ModerationRequest(boolean hidden, @Size(max = 500) String flagReason) {
    }

    public record TaskRequest(
            @NotBlank @Size(max = 190) String title,
            @NotNull TaskType taskType,
            @Size(max = 4000) String instructions,
            List<Map<String, Object>> questions,
            @Min(1) Integer maxScore,
            OffsetDateTime dueDate,
            Boolean published) {
    }

    public record TaskResponse(
            Long taskId,
            Long activityId,
            String title,
            TaskType taskType,
            String instructions,
            List<Map<String, Object>> questions,
            int maxScore,
            OffsetDateTime dueDate,
            boolean published) {
    }

    public record SubmissionRequest(List<Integer> answers, @Size(max = 6000) String submissionText) {
    }

    public record GradeRequest(@NotNull @Min(0) Integer score, @Size(max = 4000) String feedback) {
    }

    public record AssessmentResponse(
            Long assessmentId,
            Long taskId,
            String taskTitle,
            Long activityId,
            Long youthId,
            String youthName,
            Integer score,
            Integer maxScore,
            double percentage,
            AssessmentStatus status,
            String feedback,
            List<Integer> answers,
            String submissionText,
            OffsetDateTime assessmentDate,
            String gradedByName,
            boolean late) {
    }

    public record CertificateResponse(
            Long certificateId,
            String certificateNumber,
            Long youthId,
            String youthName,
            Long activityId,
            String activityTitle,
            String achievement,
            LocalDate issueDate,
            boolean revoked) {
    }

    public record ProgressResponse(
            Long activityId,
            String activityTitle,
            EnrollmentStatus enrollmentStatus,
            long totalSessions,
            long attendedSessions,
            Double attendancePercent,
            int gradedAssessments,
            double assessmentAverage,
            int passMark,
            int minAttendancePercent,
            boolean passedAssessments,
            boolean passedAttendance,
            boolean completed,
            boolean levelTwoEligible,
            List<AssessmentResponse> assessments,
            List<CertificateResponse> certificates) {
    }

    /** Facilitator roster row: enrollment plus attendance/results percentages and last activity timestamp. */
    public record ParticipantRow(
            Long youthId,
            String name,
            String country,
            EnrollmentStatus status,
            Double attendancePercent,
            Double resultsPercent,
            OffsetDateTime lastActivityAt) {
    }

    /** Summary returned after running an activity completion workflow. */
    public record ActivityCompletionSummary(int certificatesIssued, int participantsCompleted) {
    }

    /** Per-task result row inside GET /api/v1/me/results. */
    public record TaskResult(
            Long taskId,
            String title,
            Integer score,
            Integer maxScore,
            Double percentage,
            AssessmentStatus status) {
    }

    /** Participant-facing results overview per activity (GET /api/v1/me/results). */
    public record MyResultsResponse(
            Long activityId,
            String activityTitle,
            Double overallPercentage,
            int gradedTasks,
            int totalTasks,
            List<TaskResult> tasks) {
    }

    /** Body for POST /api/v1/posts/{id}/flag — reason is optional. */
    public record FlagRequest(@Size(max = 500) String reason) {
    }

    /** Body for PUT /api/v1/admin/posts/{id}/hide. */
    public record HidePostRequest(@NotNull Boolean hidden) {
    }

    /** Moderation queue row (GET /api/v1/admin/moderation): flagged, not-yet-hidden posts. */
    public record ModerationQueueResponse(
            Long postId,
            String threadTopic,
            String activityTitle,
            String authorName,
            String content,
            String flagReason,
            OffsetDateTime postedAt,
            boolean hidden) {
    }
}
