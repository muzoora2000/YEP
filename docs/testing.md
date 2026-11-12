# YEP – Testing Guide

> This document explains how to run the backend tests and describes the eleven acceptance
> scenarios specified in the project brief.

---

## 1. How to run backend tests

### Prerequisites
- Java 17 in PATH (`java -version` → 17.x)
- Maven 3.9+ in PATH (`mvn -v`)

### Environment
No database is required. Tests run with the `test` Spring profile, which points Spring Data JPA at an
in-memory H2 database in PostgreSQL compatibility mode, and Flyway applies the same migrations used in
production so the schema under test matches deployment.

```bash
# from the repository root
mvn -f backend/pom.xml test                       # run all JUnit 5 tests
mvn -f backend/pom.xml verify                     # compile + test + package
mvn -f backend/pom.xml test -Dtest=AuthFlowTest   # run a single test class
```

### Frontend build check
```bash
cd frontend
npm install
npm run typecheck   # tsc --noEmit
npm run build       # must exit 0
```

---

## 2. Test classes present

All test classes live in `backend/src/test/java/com/yep/`. There are 22 tests in total.

| Class | Type | What it covers |
|---|---|---|
| `AuthFlowTest` | MockMvc HTTP | register/login, JWT + `GET /auth/me`, pending facilitator blocked until admin approval, participant blocked from admin routes, `GET /users/{id}` self-or-admin rule, duplicate email, single-use hashed reset tokens, forgot-password non-disclosure, facilitator→mentor role switch |
| `LearningFlowTest` | Service | Level One journey end-to-end (enrol, check-in, post, submit, grade, certificate, `levelTwoEligible`), capacity/waitlist promotion on cancel, and the `ActivityCompletionWorkflow` template method |
| `ProjectFlowTest` | Service | Level Two apply/select flow, team build, mentor assignment, project milestones, submit, mentor review (ownership enforced), support visibility, complete, AI-usage declaration rule |
| `PartnerFlowTest` | Service | partner publishes an opportunity, youth interest triggers a notification, partner supports an approved project, admin dashboard/audit/settings, unverified partner blocked |
| `SchemaValidationTest` | JPA | entities match the Flyway migrations (tables and columns) |

---

## 3. The eleven acceptance scenarios

The following table maps each scenario from the project specification to the relevant endpoint,
the data to set up, and whether the test has been verified against the live system.

| # | Scenario | Actor | Endpoint(s) | Setup | Expected result | Status |
|---|---|---|---|---|---|---|
| 1 | **Register and log in** | Youth Participant | `POST /api/v1/auth/register` → `POST /api/v1/auth/login` | Fresh email, age 22, role PARTICIPANT | 201 with userId; 200 with JWT token + cookie `yep_token` | ✅ Pass |
| 2 | **Enrol in a Level 1 activity** | Youth Participant | `POST /api/v1/activities/{id}/enroll` | Activity status = PUBLISHED, youth ACTIVE | 201 enrollment with status ENROLLED (Level One enrols directly) | ✅ Pass |
| 3 | **Waitlist when activity is full** | Youth Participant | `POST /api/v1/activities/{id}/enroll` | Activity capacity = 1, 1 already ENROLLED | 201 enrollment with status WAITLISTED | ✅ Pass |
| 4 | **Facilitator selects an applicant** | Facilitator | `PUT /api/v1/enrollments/{id}/decision` (body `{"decision":"SELECT"}`) | Enrollment in APPLIED state | status → ENROLLED; youth notified IN_APP | ✅ Pass |
| 5 | **Submit a quiz and get auto-graded** | Youth Participant | `POST /api/v1/tasks/{id}/submit` | AssessmentTask type=QUIZ, 2 questions, correctIndex known | Assessment GRADED, score computed, gradedBy=null | ✅ Pass |
| 6 | **Certificate issued after completing activity** | Facilitator | `POST /api/v1/activities/{id}/complete` (runs the completion workflow) | Enrolled roster; attendance ≥ minAttendancePercent, score ≥ passMark | Activity → COMPLETED; certificate + PDF issued to each eligible youth, notification sent | ✅ Pass |
| 7 | **Form teams (Balanced strategy)** | Facilitator / Admin | `POST /api/v1/teams` then `POST /api/v1/teams/{id}/members` | Level 2 activity, 3+ enrolled youth | Teams created with balanced sizes, one leader per team | ✅ Pass |
| 8 | **Submit impact project** | Youth (Team Leader) | `POST /api/v1/projects/{id}/submit` | Project status=DRAFT, all required fields filled | status → SUBMITTED; mentor notified | ✅ Pass |
| 9 | **Mentor reviews and approves project** | Mentor | `POST /api/v1/projects/{id}/reviews` with decision=APPROVED | Project in SUBMITTED state, caller is assigned mentor | status → APPROVED; team leader notified | ✅ Pass |
| 10 | **Partner views interested talent** | Partner Organisation | `GET /api/v1/partner/opportunities/{id}/interests` | Youth opted in via `POST /api/v1/opportunities/{id}/interest` | Interested youth shown (name + message); no email/phone exposed | ✅ Pass |
| 11 | **Public certificate verification** | Anonymous | `GET /api/v1/public/certificates/{number}` | Certificate exists | JSON with youth name, activity title, issue date, achievement — no PII leaked | ✅ Pass |

---

## 4. OOP patterns verified in tests

| Pattern | Test | What is checked |
|---|---|---|
| **Encapsulation** | `LearningFlowTest.capacityWaitlistPromotesNextParticipantWhenSomeoneCancels` | `Enrollment.cancel()` / `confirm()` are the only paths that change enrollment status |
| **Inheritance + Polymorphism** | `ProjectFlowTest` + `LearningFlowTest` | `User` subclasses (Participant/Facilitator/Mentor/Partner/Admin); Level One enrols while Level Two applies |
| **Factory** | `PartnerFlowTest` | `NotificationService.notify(...)` builds `EmailNotification` / `InAppNotification` / `SmsNotification` from the type |
| **Strategy** | `ProjectFlowTest` (team build) + `LearningFlowTest` (completion) | `BalancedTeamStrategy` sizes teams; the completion service selects a workflow by activity level |
| **Template Method** | `LearningFlowTest.completionWorkflowIssuesCertificatesForEligibleRoster` | `ActivityCompletionWorkflow` fixes the algorithm; `LevelOne`/`LevelTwo` vary eligibility + achievement text |
| **Repository / Service** | All flow tests | Services operate on domain objects; controllers are thin wrappers |
| **Global error body** | `AuthFlowTest` | Every exception returns `{timestamp, status, code, message, fieldErrors}` |

---

## 5. Demo accounts (all passwords: `Demo1234!`)

Seeded by `DemoSeeder` when the `demo` profile is active (`SPRING_PROFILES_ACTIVE=prod,demo`).

| Email | Role | Notes |
|---|---|---|
| `facilitator@yep.demo` | FACILITATOR | Dr. Grace Omondi — owns 2 Level One + 1 Level Two demo activities |
| `mentor@yep.demo` | MENTOR | James Mwangi — assigned to the demo team |
| `alice@yep.demo` | PARTICIPANT | Uganda, level-two eligible, team LEADER |
| `bob@yep.demo` | PARTICIPANT | Kenya, level-two eligible, team member |
| `carol@yep.demo` | PARTICIPANT | Tanzania, enrolled in Level One |
| `david@yep.demo` | PARTICIPANT | Rwanda, enrolled in Level One |
| `eve@yep.demo` | PARTICIPANT | Uganda, enrolled in Level One |
| `frank@yep.demo` | PARTICIPANT | Kenya, enrolled in Level One |
| `university@yep.demo` | PARTNER | Makerere University (verified, type UNIVERSITY) |
| `employer@yep.demo` | PARTNER | TechHub EA (verified, type EMPLOYER) |

The demo also seeds the "Innovators Afrika" team with one DRAFT impact project and three milestones.
The first administrator is **not** fixed: it is created from the `ADMIN_EMAIL` / `ADMIN_PASSWORD`
environment variables on first boot (for example `admin@yep.demo` when the deployment sets those values).

Activate with `spring.profiles.active=demo` or the `SPRING_PROFILES_ACTIVE=demo` environment variable.

---

## 6. Known limitations

- **Email sending**: SMTP credentials are optional. If `MAIL_HOST` is not set, emails are logged
  to stdout but not delivered. Configure an SMTP provider (e.g., Mailgun, SendGrid) for real delivery.
- **SMS**: Africa's Talking sandbox credentials are required for real SMS. Without them,
  SMS notifications are logged but not sent.
- **PDF certificates**: Generated on first certificate issue. The `GET /api/v1/public/certificates/{number}/pdf`
  endpoint streams the file. QR code embeds the public verification URL.
- **Video hosting**: YEP does not host video. Session `meetingLink` stores a third-party URL
  (Google Meet, Zoom, Jitsi). The platform only links to it.
- **Render free tier cold start**: The backend may take 30–60 seconds to wake from sleep on
  the first request. The `ServerWakingBanner` component in the frontend retries up to 5 times
  with exponential backoff and shows a friendly message during this period.
