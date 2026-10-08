# YEP - Entity Relationship Diagram

Source of truth: `backend/src/main/resources/db/migration/V1__init_schema.sql`
(applied by Flyway in the `prod` profile, and by `test` in CI).

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR role "discriminator: PARTICIPANT|FACILITATOR|MENTOR|ADMIN"
        VARCHAR country
        VARCHAR phone
        BOOLEAN consent_given
        VARCHAR account_status "ACTIVE|SUSPENDED|PENDING"
        INTEGER age "participant"
        VARCHAR skill_level "participant"
        VARCHAR expertise_area "facilitator"
        VARCHAR expertise_topics "facilitator"
        INTEGER sessions_facilitated "facilitator"
        VARCHAR mentorship_areas "mentor"
        VARCHAR permissions "admin"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    virtual_exchanges {
        BIGINT id PK
        VARCHAR title
        VARCHAR description
        VARCHAR exchange_level "discriminator: LEVEL_ONE|LEVEL_TWO"
        DATE start_date
        DATE end_date
        VARCHAR status "DRAFT|OPEN|ACTIVE|COMPLETED|CANCELLED"
        BIGINT organiser_id
        VARCHAR topic "level one"
        INTEGER max_participants "level one"
        VARCHAR session_link "level one"
        BIGINT facilitator_id "level one"
        VARCHAR focus_area "level two"
        INTEGER max_teams "level two"
        BOOLEAN applications_open "level two"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    enrollments {
        BIGINT id PK
        BIGINT participant_id FK
        BIGINT virtual_exchange_id FK
        TIMESTAMPTZ enrolled_at
        VARCHAR completion_status "ENROLLED|IN_PROGRESS|COMPLETED|WITHDRAWN"
        TIMESTAMPTZ completed_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    teams {
        BIGINT id PK
        VARCHAR name
        VARCHAR description
        BIGINT level_two_exchange_id FK
        BIGINT mentor_id FK
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    team_members {
        BIGINT id PK
        BIGINT team_id FK
        BIGINT participant_id FK
        VARCHAR member_role "LEAD|MEMBER"
        TIMESTAMPTZ joined_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    impact_projects {
        BIGINT id PK
        VARCHAR title
        VARCHAR problem_statement
        VARCHAR proposed_solution
        BIGINT team_id FK "unique - one project per team"
        VARCHAR status "DRAFT|SUBMITTED|UNDER_REVIEW|APPROVED|COMPLETED|REJECTED"
        TIMESTAMPTZ submission_date
        BOOLEAN ai_assisted
        VARCHAR ai_usage_note
        VARCHAR review_feedback
        BIGINT reviewed_by_id
        TIMESTAMPTZ reviewed_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    assessments {
        BIGINT id PK
        VARCHAR target_type "PARTICIPANT|TEAM"
        BIGINT participant_target_id FK "set when PARTICIPANT"
        BIGINT team_target_id FK "set when TEAM"
        BIGINT assessor_id FK
        VARCHAR criteria
        INTEGER score "0..100"
        VARCHAR feedback
        TIMESTAMPTZ assessment_date
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    certificates {
        BIGINT id PK
        BIGINT recipient_id FK
        VARCHAR title
        TIMESTAMPTZ issue_date
        VARCHAR verification_code UK
        VARCHAR related_activity_or_project
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    messages {
        BIGINT id PK
        BIGINT sender_id FK
        BIGINT recipient_id FK
        BIGINT team_id FK
        BIGINT exchange_id FK
        VARCHAR subject
        VARCHAR body
        BOOLEAN is_read
        TIMESTAMPTZ sent_at
        BOOLEAN ai_generated
        VARCHAR ai_flag_reason
        VARCHAR ai_moderation_status "PENDING|APPROVED|BLOCKED"
        VARCHAR message_type "discriminator: DIRECT|TEAM|SYSTEM|ANNOUNCEMENT"
        VARCHAR scope "announcement: ALL_USERS|EXCHANGE_COHORT|TEAM"
        BOOLEAN published "announcement"
        BOOLEAN requires_human_approval "announcement"
        BIGINT approved_by_id
        TIMESTAMPTZ approved_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    users ||--o{ enrollments : "participant"
    virtual_exchanges ||--o{ enrollments : "exchange"
    users ||--o{ teams : "mentor"
    virtual_exchanges ||--o{ teams : "level two"
    teams ||--o{ team_members : "membership"
    users ||--o{ team_members : "participant"
    teams ||--o| impact_projects : "one project per team (UK)"
    users ||--o{ assessments : "assessor"
    users ||--o{ assessments : "participant target"
    teams ||--o{ assessments : "team target"
    users ||--o{ certificates : "recipient"
    users ||--o{ messages : "sender"
    users ||--o{ messages : "recipient"
    teams ||--o{ messages : "team message"
    virtual_exchanges ||--o{ messages : "cohort announcement"
```

## Notes

1. **Three `SINGLE_TABLE` hierarchies** are stored in one table each, with the JPA
   discriminator column (`role`, `exchange_level`, `message_type`) doing double duty:
   - `users` - `Participant`, `Facilitator`, `Mentor`, `Admin`
   - `virtual_exchanges` - `LevelOneExchange` (topic/session columns), `LevelTwoExchange` (focus/team columns)
   - `messages` - direct/team/system rows ignore the announcement-only columns
2. **Polymorphic assessment target** - `assessments` carries both FKs plus a
   `CHECK (ck_assessments_target)` so exactly one is populated, matching the
   `Assessable` interface in the class model.
3. **Certification rules** - `impact_projects.team_id` is unique (one project per team)
   and `certificates.verification_code` is unique, which powers the public
   `GET /api/v1/certificates/verify/{code}` endpoint.
4. **Indexes** are declared for every join column, every enum used in filters
   (`role`, `account_status`, `exchange_level`, `status`, `completion_status`,
   `message_type`) and the ordering columns used by list endpoints
   (`created_at`, `start_date`, `issue_date`).
5. **Audit columns** - every table has `created_at`; `updated_at` is maintained by
   `BaseEntity` in JPA.
