# YEP - UML Class Diagram

The domain model lives in `backend/src/main/java/com/yep/domain`. It is deliberately built
around the four OOP pillars; each one is called out below the diagram.

```mermaid
classDiagram
    direction TB

    class BaseEntity {
        <<abstract>>
        #Long id
        #OffsetDateTime createdAt
        #OffsetDateTime updatedAt
        +getId() Long
    }

    class User {
        <<abstract>>
        -String fullName
        -String email
        -String passwordHash
        -Role role
        -String country
        -String phone
        -boolean consentGiven
        -AccountStatus accountStatus
        +getAuthorities() Collection~GrantedAuthority~
        +getPassword() String
        +getUsername() String
        +isEnabled() boolean
        +hasRole(Role) boolean
        +isAdmin() boolean
        +getNotificationAddress() String
        +getNotificationLabel() String
    }

    class Participant {
        -Integer age
        -SkillLevel skillLevel
        +getAssessmentTargetType() AssessmentTargetType
        +getAssessmentLabel() String
        +getCertificateTitle() String
        +qualifiesForCertificate() boolean
        +getCertificateRecipients() List~Participant~
    }

    class Facilitator {
        -String expertiseArea
        -String expertiseTopics
        -int sessionsFacilitated
    }

    class Mentor {
        -String mentorshipAreas
    }

    class Admin {
        -String permissions
        +permissionSet() Set~String~
    }

    class VirtualExchange {
        <<abstract>>
        -String title
        -String description
        -ExchangeLevel level
        -LocalDate startDate
        -LocalDate endDate
        -ExchangeStatus status
        -Long organiserId
        -Set~Enrollment~ enrollments
        +getLevel() ExchangeLevel*
        +getSummaryLabel() String*
        +isAtCapacity() boolean
        +enrolledCount() int
        +isOpenForEnrollment() boolean
    }

    class LevelOneExchange {
        -LevelOneTopic topic
        -Integer maxParticipants
        -String sessionLink
        -Long facilitatorId
        +getLevel() ExchangeLevel
        +getSummaryLabel() String
        +isAtCapacity() boolean
        +hasSeatsFor(int) boolean
        +acceptsEnrollmentOn(LocalDate) boolean
    }

    class LevelTwoExchange {
        -LevelTwoFocusArea focusArea
        -Integer maxTeams
        -boolean applicationsOpen
        -Set~Team~ teams
        +getLevel() ExchangeLevel
        +getSummaryLabel() String
        +isAtCapacity() boolean
    }

    class Enrollment {
        -Participant participant
        -VirtualExchange virtualExchange
        -OffsetDateTime enrolledAt
        -CompletionStatus completionStatus
        -OffsetDateTime completedAt
        +markCompleted() void
        +withdraw() void
    }

    class Team {
        -String name
        -String description
        -LevelTwoExchange levelTwoExchange
        -Mentor mentor
        -Set~TeamMember~ teamMembers
        -ImpactProject impactProject
        -Set~Assessment~ assessments
        +addMember(Participant, TeamMemberRole) TeamMember
        +removeMember(Participant) void
        +isMember(Participant) boolean
        +findTeamLead() Participant
        +memberCount() int
        +getAssessmentTargetType() AssessmentTargetType
        +qualifiesForCertificate() boolean
        +getNotificationAddress() String
    }

    class TeamMember {
        -Team team
        -Participant participant
        -TeamMemberRole memberRole
        -OffsetDateTime joinedAt
    }

    class ImpactProject {
        -String title
        -String problemStatement
        -String proposedSolution
        -Team team
        -ProjectStatus status
        -OffsetDateTime submissionDate
        -boolean aiAssisted
        -String aiUsageNote
        -String reviewFeedback
        -Long reviewedById
        -OffsetDateTime reviewedAt
        +draftFor(Team, String) ImpactProject$
        +submit() void
        +startReview() void
        +approve(String, User) void
        +complete() void
        +canBeSubmitted() boolean
        +getCertificateTitle() String
        +qualifiesForCertificate() boolean
        +getCertificateRecipients() List~Participant~
    }

    class Assessment {
        -AssessmentTargetType targetType
        -Participant participantTarget
        -Team teamTarget
        -User assessor
        -String criteria
        -Integer score
        -String feedback
        -OffsetDateTime assessmentDate
        +of(Assessable, User, String, int, String) Assessment$
        +resolveTarget() Assessable
        +isPass() boolean
        +affectedParticipants() List~Participant~
    }

    class Certificate {
        -Participant recipient
        -String title
        -OffsetDateTime issueDate
        -String verificationCode
        -String relatedActivityOrProject
        +issuedTo(Participant, String, String, String) Certificate$
        +isIssuedOnOrAfter(OffsetDateTime) boolean
    }

    class Message {
        <<abstract>>
        -User sender
        -User recipient
        -Team team
        -VirtualExchange exchange
        -String subject
        -String body
        -boolean read
        -OffsetDateTime sentAt
        -boolean aiGenerated
        -String aiFlagReason
        -AiModerationStatus aiModerationStatus
        +markRead() void
    }

    class DirectMessage {
        +getMessageType() MessageType
    }

    class TeamMessage {
        +getMessageType() MessageType
    }

    class SystemMessage {
        +getMessageType() MessageType
    }

    class Announcement {
        -AnnouncementScope scope
        -boolean published
        -boolean requiresHumanApproval
        -Long approvedById
        -OffsetDateTime approvedAt
        +byUser(User, AnnouncementScope, String, String, boolean) Announcement$
        +approveBy(User) void
    }

    class Assessable {
        <<interface>>
        +getId() Long
        +getAssessmentTargetType() AssessmentTargetType
        +getAssessmentLabel() String
        +getAssessmentRecipients() List~Participant~
    }

    class Certifiable {
        <<interface>>
        +getCertificateTitle() String
        +getCertificateReference() String
        +qualifiesForCertificate() boolean
        +getCertificateRecipients() List~Participant~
    }

    class Notifiable {
        <<interface>>
        +getNotificationAddress() String
        +getPreferredChannel() NotificationChannel
        +getNotificationLabel() String
    }

    BaseEntity <|-- User
    User <|-- Participant : role PARTICIPANT
    User <|-- Facilitator : role FACILITATOR
    User <|-- Mentor : role MENTOR
    User <|-- Admin : role ADMIN

    BaseEntity <|-- VirtualExchange
    VirtualExchange <|-- LevelOneExchange : exchange_level LEVEL_ONE
    VirtualExchange <|-- LevelTwoExchange : exchange_level LEVEL_TWO

    BaseEntity <|-- Message
    Message <|-- DirectMessage : message_type DIRECT
    Message <|-- TeamMessage : message_type TEAM
    Message <|-- SystemMessage : message_type SYSTEM
    Message <|-- Announcement : message_type ANNOUNCEMENT

    BaseEntity <|-- Enrollment
    BaseEntity <|-- Team
    BaseEntity <|-- TeamMember
    BaseEntity <|-- ImpactProject
    BaseEntity <|-- Assessment
    BaseEntity <|-- Certificate

    User "1" <-- "0..*" Enrollment : participant
    VirtualExchange "1" <-- "0..*" Enrollment : virtualExchange
    LevelTwoExchange "1" <-- "0..*" Team : levelTwoExchange
    Mentor "0..1" <-- "0..*" Team : mentor
    Team "1" <-- "1..*" TeamMember : team
    Participant "1" <-- "0..*" TeamMember : participant
    Team "1" <-- "0..1" ImpactProject : impactProject (1:1, unique team_id)
    User "1" <-- "0..*" Assessment : assessor
    Participant "0..1" <-- "0..*" Assessment : participantTarget
    Team "0..1" <-- "0..*" Assessment : teamTarget
    Participant "1" <-- "0..*" Certificate : recipient
    User "0..1" <-- "0..*" Message : sender
    User "0..1" <-- "0..*" Message : recipient
    Team "0..1" <-- "0..*" Message : team
    VirtualExchange "0..1" <-- "0..*" Message : exchange

    User ..|> UserDetails : Spring Security
    User ..|> Notifiable
    Team ..|> Notifiable
    Participant ..|> Assessable
    Team ..|> Assessable
    Participant ..|> Certifiable
    ImpactProject ..|> Certifiable
```

## How the four pillars show up in the code

| Pillar | Where it is applied |
|---|---|
| **Encapsulation** | Every field in `domain/**` is `private` with getters/setters; state changes go through intent-revealing methods (`ImpactProject.submit()`, `ImpactProject.approve(...)`, `Enrollment.markCompleted()`, `Team.addMember(...)`) so invalid transitions are impossible from outside the class. `passwordHash` is never exposed as a plain field; `User.getPassword()` implements the Spring Security contract instead. |
| **Inheritance** | `SINGLE_TABLE` hierarchies: `User → Participant / Facilitator / Mentor / Admin`, `VirtualExchange → LevelOneExchange / LevelTwoExchange`, `Message → DirectMessage / TeamMessage / SystemMessage / Announcement`. All three share `BaseEntity` (id, audit columns). |
| **Polymorphism** | `VirtualExchange.getSummaryLabel()` and `isAtCapacity()` are abstract and behave differently per level, so `ExchangeService` and the `ExchangeResponse.of(...)` mapper never branch on concrete types. `Assessable` lets `AssessmentService` score a `Participant` or a `Team`; `Certifiable` lets `CertificateService` issue documents for a completed Level One activity or a completed impact project; `Notifiable` lets the notifier reach a `User` or a `Team` uniformly. |
| **Abstraction / interfaces** | `Assessable`, `Certifiable`, `Notifiable` in `domain/interfaces` hide the concrete targets; `UserDetails` hides YEP accounts from Spring Security (`YepUserDetailsService`); `PageResponse<T>` abstracts pagination across every list endpoint; the frontend mirrors the same contract with typed DTOs in `frontend/src/lib/types.ts`. |

## Design notes

- **Discriminators double as business data** (`role`, `exchange_level`, `message_type`), which keeps the class hierarchy and the ER schema in lockstep - see [`ER_DIAGRAM.md`](./ER_DIAGRAM.md).
- **Lazy collections** (`enrollments`, `teams`, `teamMembers`) are loaded through repositories; services never leak JPA-managed collections into DTOs.
- **Invariants live in the domain, not the controller**: `ImpactProject.qualifiesForCertificate()`, `LevelOneExchange.hasSeatsFor(int)`, `Assessment.isPass()` and `Team.hasTeamLead()` are reusable by services, tests and (through the API) the frontend.
