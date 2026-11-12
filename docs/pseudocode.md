# YEP – Pseudocode for 11 Specified Functions

> This file documents the logical flow of the eleven key functions listed in the project specification.
> The goal is to show the OOP pattern at work in plain English before reading the Java source.

---

## 1. `User.register()` — `AuthService.register()`

```
FUNCTION register(name, email, password, role, phone, age?, country?, …)
  VALIDATE fields with Bean Validation (@NotBlank, @Email, @Size)
  IF userRepository.existsByEmail(email) THEN
    THROW BadRequestException("EMAIL_TAKEN")
  END IF

  -- Encapsulation: User status is PENDING until admin approves
  hash ← BCrypt.hashpw(password, BCrypt.gensalt(12))

  -- Inheritance: concrete subtype created based on role
  MATCH role
    CASE PARTICIPANT → user ← new YouthParticipant(name, email, hash, age, country, digitalSkills)
    CASE FACILITATOR → user ← new Facilitator(name, email, hash, specialisation)
    CASE MENTOR      → user ← new Mentor(name, email, hash, organisation, expertise)
    CASE ADMIN       → user ← new Administrator(name, email, hash, department)
    CASE PARTNER     → user ← new PartnerOrganisation(orgName, country, orgType)
  END MATCH

  SET user.status ← PENDING
  SET user.createdAt ← now()
  SAVE user via userRepository (JPA JOINED inheritance writes two rows: users + subtype)

  -- Notification polymorphism: NotificationFactory decides channel
  notificationService.notify(user, "Welcome to YEP …", null, IN_APP)
  RETURN UserProfile DTO
END FUNCTION
```

---

## 2. `User.login()` — `AuthService.login()`

```
FUNCTION login(email, password)
  -- Login rate limiting (encapsulation: check hidden inside LoginRateLimiter)
  IF loginRateLimiter.isBlocked(ip, email) THEN
    THROW TooManyRequestsException
  END IF

  user ← userRepository.findByEmail(email)
         OR THROW BadRequestException("INVALID_CREDENTIALS")

  IF NOT BCrypt.checkpw(password, user.passwordHash) THEN
    loginRateLimiter.recordFailure(ip, email)
    THROW BadRequestException("INVALID_CREDENTIALS")
  END IF

  IF user.status ≠ ACTIVE THEN
    THROW ForbiddenException("ACCOUNT_NOT_ACTIVE")
  END IF

  SET user.lastLoginAt ← now()
  SAVE user

  -- JWT: encapsulates identity so the stateless API can verify every request
  token ← jwtService.generate(user.userId, user.role, expiryHours)
  SET httpOnly cookie "yep_token" on response

  loginRateLimiter.reset(ip, email)
  RETURN AuthResponse(token, "Bearer", expiresInSeconds, UserProfile)
END FUNCTION
```

---

## 3. `YouthParticipant.enrollActivity()` — `ActivityService.enroll()`

```
FUNCTION enrollActivity(activityId)
  activity ← activityRepository.findById(activityId) OR THROW NotFoundException
  youth    ← youthParticipantRepository.findById(currentUserId) OR THROW ForbiddenException

  IF activity.status ≠ PUBLISHED THEN THROW BadRequestException("NOT_PUBLISHED") END IF

  IF enrollmentRepository.existsByYouthAndActivity(youth, activity) THEN
    THROW BadRequestException("ALREADY_ENROLLED")
  END IF

  IF activity.level = TWO AND NOT youth.levelTwoEligible THEN
    THROW ForbiddenException("LEVEL_TWO_NOT_ELIGIBLE")
  END IF

  -- Capacity + waitlist logic (encapsulation: status set via confirmEnrollment)
  enrolledCount ← enrollmentRepository.countByActivityAndStatus(activity, ENROLLED)
  IF activity.capacity != null AND enrolledCount >= activity.capacity THEN
    status ← WAITLISTED
  ELSE
    status ← APPLIED
  END IF

  enrollment ← new Enrollment(youth, activity, now(), status)
  SAVE enrollment

  notificationService.notify(youth, "Enrolled in " + activity.title, "/exchanges/" + activityId, IN_APP)
  RETURN EnrollmentResponse
END FUNCTION
```

---

## 4. `Enrollment.confirmEnrollment()` / `cancelEnrollment()` — `ActivityService.updateEnrollmentStatus()`

```
-- Encapsulation: enrollment status changes only through these domain methods
-- External code cannot set enrollment.status directly; it calls confirm() or cancel().

FUNCTION confirmEnrollment(enrollmentId, activityId)
  enrollment ← find enrollment by id, verify it belongs to activityId
  activityService.requireManage(enrollment.activity)   -- FACILITATOR or ADMIN only

  CALL enrollment.confirm()   -- sets this.status = ENROLLED, sets confirmDate
  SAVE enrollment

  notificationService.notify(enrollment.youth, "Your enrolment was confirmed!", link, IN_APP)
  RETURN EnrollmentResponse
END FUNCTION

FUNCTION cancelEnrollment(enrollmentId, activityId)
  enrollment ← find enrollment
  CALL enrollment.cancel()    -- sets this.status = CANCELLED
  SAVE enrollment

  -- Promote first WAITLISTED participant if capacity freed
  next ← enrollmentRepository.findFirstByActivityAndStatusOrderByDateAsc(activity, WAITLISTED)
  IF next EXISTS THEN
    CALL next.confirm()
    SAVE next
    notificationService.notify(next.youth, "You have been moved from the waitlist!", link, IN_APP)
  END IF
  RETURN EnrollmentResponse
END FUNCTION
```

---

## 5. `Assessment.evaluate()` — `AssessmentService.submit()` (quiz auto-grade)

```
FUNCTION submit(taskId, submissionText?, answers?)
  task  ← assessmentTaskRepository.findById(taskId) OR THROW NotFoundException
  youth ← current user (must be ENROLLED in the activity)

  IF assessmentRepository.existsByTaskAndYouth(task, youth) THEN
    THROW BadRequestException("ALREADY_SUBMITTED")
  END IF

  assessment ← new Assessment(task, youth, now())
  SET assessment.submissionText ← submissionText
  SET assessment.answers ← answers (JSONB)

  IF task.taskType = QUIZ THEN
    -- Polymorphism: auto-grade only for quizzes; assignments stay SUBMITTED for human review
    score ← 0
    FOR EACH question q IN task.questions, answer a IN answers DO
      IF a.selectedIndex = q.correctIndex THEN score ← score + (task.maxScore / count) END IF
    END FOR
    SET assessment.score ← score
    SET assessment.status ← GRADED
    SET assessment.gradedBy ← null   -- null signals auto-grade (Responsible AI: no AI grading)
  ELSE
    SET assessment.status ← SUBMITTED   -- awaits facilitator grading
  END IF

  SAVE assessment
  -- Certificate issuance is NOT triggered here; it runs in the activity completion workflow.
  RETURN AssessmentResponse
END FUNCTION
```

---

## 6. `CertificateService.issue()` / `issueCertificate()`

```
-- Manual issuance by a facilitator/admin: POST /activities/{id}/certificates/{youthId}
FUNCTION issue(activityId, youthId)
  activity ← activityService.find(activityId)
  activityService.requireManage(activity)          -- ownership / role check

  IF certificateRepository.existsByYouthAndActivity(youthId, activityId) THEN
    THROW ConflictException("CERTIFICATE_EXISTS")
  END IF

  enrollment ← enrollmentRepository.findByYouthAndActivity(youthId, activityId)
                OR THROW BusinessRuleException("ENROLLMENT_REQUIRED")
  youth ← enrollment.youth
  progress ← progressService.progressFor(activityId, youthId)
  IF NOT progress.passedAssessments() THEN THROW BusinessRuleException("ASSESSMENT_REQUIREMENT_NOT_MET") END IF
  IF NOT progress.passedAttendance()  THEN THROW BusinessRuleException("ATTENDANCE_REQUIREMENT_NOT_MET") END IF

  project ← projectFor(youth, activity)
  IF activity.level = TWO AND NOT isProjectComplete(project) THEN
    THROW BusinessRuleException("PROJECT_REQUIREMENT_NOT_MET")
  END IF

  certificate ← issueCertificate(youth, activity, achievementText(activity, project))
  RETURN progressService.toCertificateResponse(certificate)
END FUNCTION

-- Core issuance used by the completion workflows (the Template Method step)
FUNCTION issueCertificate(youth, activity, achievement)
  IF certificateRepository.existsByYouthAndActivity(youth, activity) THEN
    THROW ConflictException("CERTIFICATE_EXISTS")
  END IF
  certificate ← new Certificate(youth, nextNumber(), achievement)   -- number format YEP-YYYY-XXXXXX
  certificate.setActivity(activity)
  project ← projectFor(youth, activity)
  IF project ≠ null THEN certificate.setProject(project) END IF
  SAVE certificate

  IF activity.level = ONE THEN youth.setLevelTwoEligible(true); SAVE youth END IF

  enrollment ← enrollmentRepository.findByYouthAndActivity(youth, activity)
  IF enrollment ≠ null AND enrollment.status ≠ COMPLETED THEN enrollment.completeEnrollment(); SAVE END IF

  notificationService.notify(youth, "Your certificate is ready: " + certificate.number,
                             "/certificates/" + certificate.number, IN_APP)
  auditService.record(currentUser, "CERTIFICATE_ISSUED", "Certificate", certificate.id, certificate.number)
  RETURN certificate
END FUNCTION

-- isEligible(participant, activity): the base rule used by LevelOneCompletionWorkflow —
--   no existing certificate AND enrollment.status = ENROLLED AND passedAssessments AND passedAttendance.
-- LevelTwoCompletionWorkflow additionally requires the team project to be APPROVED or COMPLETED.
```

---

## 7. `BalancedTeamStrategy.formTeams()` — Strategy pattern

```
-- Strategy interface: TeamFormationStrategy.formTeams(unassigned, activity)
-- BalancedTeamStrategy is the concrete strategy; team formation can be swapped by injecting a
-- different TeamFormationStrategy. Teams can also be built manually via POST /teams.

FUNCTION BalancedTeamStrategy.formTeams(unassigned, activity)
  total ← unassigned.size()
  IF total = 0 THEN RETURN [] END IF

  maxSize ← activity.maxTeamSize
  minSize ← activity.minTeamSize
  IF total < minSize THEN
    THROW BusinessRuleException("NOT_ENOUGH_PARTICIPANTS")
  END IF

  -- choose a team count that keeps every team within [minSize, maxSize]
  byCapacity ← CEIL(total / maxSize)
  byMinimum  ← FLOOR(total / minSize)
  teamCount  ← MAX(1, MIN(byCapacity, byMinimum))

  -- sort by country then digital-skill level, then deal round-robin so each team mixes profiles
  sorted ← unassigned SORTED BY country, digitalSkills
  buckets ← teamCount empty lists
  FOR EACH youth IN sorted DO
    buckets[next % teamCount].add(youth); next ← next + 1
  END FOR

  teams ← []
  FOR EACH bucket IN buckets DO
    team ← new Team(activity, "Team " + index, activity.facilitator)
    first ← true
    FOR EACH youth IN bucket DO
      team.addMember(youth, IF first THEN LEADER ELSE MEMBER)  -- encapsulation: addMember validates capacity
      first ← false
    END FOR
    team.refreshStatus(maxSize)
    teams.add(team)
  END FOR
  RETURN teams
END FUNCTION
```

---

## 8. `ImpactProject.submitProject()` — `ProjectService.submit()`

```
FUNCTION submitProject(projectId)
  project ← find project, verify caller is a team member or admin
  -- Encapsulation: status transitions guarded inside submitProject()
  IF project.status ≠ DRAFT AND project.status ≠ CHANGES_REQUESTED THEN
    THROW BadRequestException("CANNOT_SUBMIT")
  END IF

  CALL project.submit()   -- sets status = SUBMITTED, submittedAt = now()
  SAVE project

  -- Notify mentor
  mentor ← project.team.mentor
  IF mentor EXISTS THEN
    notificationService.notify(mentor, "Project "" + project.title + "" submitted for review", link, IN_APP)
  END IF
  RETURN ProjectResponse
END FUNCTION
```

---

## 9. Review decision handling — `ProjectService.review()`

```
FUNCTION review(projectId, decision, feedback)
  project ← find project
  mentor  ← current user (must be assigned mentor OR admin)

  review ← new ProjectReview(project, mentor, decision, feedback, now())
  SAVE review

  -- Polymorphism: decision drives status change without instanceof
  MATCH decision
    CASE APPROVED           → CALL project.approve()       -- sets status = APPROVED
    CASE CHANGES_REQUESTED  → CALL project.requestChanges() -- sets status = CHANGES_REQUESTED
    CASE COMMENT            → (status unchanged)
  END MATCH
  SAVE project

  -- Notify team leader
  leader ← project.team.members.find(m → m.role = LEADER)
  IF leader EXISTS THEN
    msg ← MATCH decision
            CASE APPROVED          → "🎉 Your project has been approved!"
            CASE CHANGES_REQUESTED → "📝 Reviewer requested changes on your project."
            CASE COMMENT           → "💬 New comment on your project."
          END MATCH
    notificationService.notify(leader.youth, msg, "/projects/" + projectId, IN_APP)
  END IF
  RETURN ReviewResponse
END FUNCTION
```

---

## 10. `NotificationService.notify()` — Factory + Polymorphism

```
FUNCTION notify(recipient, message, link, type)
  IF recipient = null THEN RETURN null END IF

  -- Factory pattern: NotificationFactory.create(type) returns the right subclass
  notification ← MATCH type
    CASE EMAIL  → new EmailNotification(recipient, message, link)
    CASE SMS    → new SmsNotification(recipient, message, link)
    DEFAULT     → new InAppNotification(recipient, message, link)
  END MATCH

  SAVE notification   -- persisted to notification table (single-table inheritance via type column)

  -- Polymorphism: send() is abstract on Notification; each subclass sends differently
  notification.send()
    EmailNotification.send()   → emailGateway.send(email, subject, body)
    SmsNotification.send()     → africasTalking.send(phone, text)
    InAppNotification.send()   → (persisted record is enough; frontend polls /notifications/me)

  RETURN NotificationResponse
END FUNCTION
```

---

## 11. Attendance % calculation — `ProgressService.attendancePercent()`

```
FUNCTION attendancePercent(youthId, activityId)
  sessions ← sessionRepository.findByActivityId(activityId)
              filtered to status ≠ CANCELLED

  IF sessions.size() = 0 THEN RETURN 100   -- no sessions = 100% by default

  attended ← attendanceRepository.countByYouthIdAndSessionIn(youthId, sessions, status = PRESENT)
              + attendanceRepository.countByYouthIdAndSessionIn(youthId, sessions, status = EXCUSED)

  percent ← (attended / sessions.size()) * 100
  RETURN ROUND(percent, 1)
END FUNCTION
```
