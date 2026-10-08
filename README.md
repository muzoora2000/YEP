# YEP - Youth Digital Empowerment and Virtual Exchange Platform

A production-ready full-stack platform for running **Level One** (large-group online learning) and
**Level Two** (small-team impact projects) virtual exchanges for young people in Uganda and East
Africa: exchanges, teams, impact projects, assessments, messaging, verifiable certificates and a
responsible-AI assistant - mobile-first and usable on low bandwidth.

| Layer | Stack |
|---|---|
| Backend | Java 17, Spring Boot 3.x, Spring Security (JWT + httpOnly cookie), JPA/Hibernate, Flyway, PostgreSQL (Neon), Maven, Docker, JUnit 5 |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS, React Query, React Hook Form + Zod, PWA (service worker + manifest) |
| Docs | [UML class diagram](docs/UML_CLASS_DIAGRAM.md), [ER diagram](docs/ER_DIAGRAM.md), [deployment guide](docs/DEPLOYMENT.md) |

## Repository layout

```
.
├── YEP_Full_Stack_AI_Build_Prompt.docx   # specification
├── backend/                              # Spring Boot API ("com.yep")
│   ├── src/main/java/com/yep/
│   │   ├── config/       # SecurityConfig, CORS, data seeder
│   │   ├── controller/   # REST layer (@RequestMapping /api/v1/...)
│   │   ├── dto/          # request/response contracts + PageResponse
│   │   ├── domain/       # entity model: user, exchange, team, project,
│   │   │                 #   assessment, certificate, message, interfaces, enums
│   │   ├── repository/   # Spring Data JPA
│   │   ├── security/     # JwtService, YepUserDetailsService, filters
│   │   └── service/      # business rules + policies (EnrollmentPolicy, ...)
│   ├── src/main/resources/
│   │   ├── application*.yml   # dev (default), h2, prod, test
│   │   └── db/migration/      # Flyway V1__init_schema.sql
│   ├── src/test/java/com/yep/ # ApiFlowTest, SchemaValidationTest (18 tests)
│   └── Dockerfile             # multi-stage Maven -> JRE 17
├── frontend/                                 # Next.js client
│   └── src/
│       ├── app/        # /, login, register, dashboard, exchanges(+[id]),
│       │               # teams(+[id]), projects(+[id]), assessments, messages,
│       │               # certificates/[code], certificates/verify, admin, offline
│       ├── components/ # RequireAuth, Navbar, Providers, ui/* (Card, Badge, Button, Form, Feedback)
│       └── lib/        # api client (in-memory JWT), auth context, zod schemas, DTO types
└── docs/               # UML, ER and deployment documentation
```

## Features

- **Accounts** - register as Participant (16-40, consent required), Facilitator, Mentor or Admin;
  BCrypt hashes, JWT in the response body *and* an httpOnly `yep_token` cookie, role-based routes
  and `@PreAuthorize` on every write endpoint.
- **Virtual exchanges** - filter by level, topic, focus area, status, date range and free text,
  paginated server-side; Level One sessions carry a topic/session link and capacity, Level Two
  programmes carry a focus area, max teams and applications flag.
- **Enrolment & teams** - self-service enrolment with per-level rules (`EnrollmentPolicy`),
  create/join teams, mentor assignment, team membership through an explicit join entity.
- **Impact projects** - draft -> submit -> human review (`START` / `APPROVE` / `REQUEST_CHANGES`)
  -> complete, with AI-disclosure fields; one project per team (unique constraint).
- **Assessments** - facilitators/mentors score participants or teams against a criterion (0-100,
  pass at 60+); participants read feedback about themselves.
- **Certificates** - issued automatically for qualifying Level One activity and completed projects,
  each with a unique verification code checked publicly at `GET /api/v1/certificates/verify/{code}`.
- **Messaging** - direct, team and system messages plus announcements with audience scope
  (`ALL_USERS` / `EXCHANGE_COHORT` / `TEAM`), unread counts, and a human approval queue.
- **Responsible AI** - `POST /api/v1/ai/suggest` drafts message/announcement/feedback text and
  `POST /api/v1/ai/suggest|moderate` label output as AI-generated; nothing is published without a
  person approving it, and AI-assisted projects must record how AI was used.
- **PWA** - installable, offline shell (`/offline`), `sw.js` caches the shell only and never caches
  API responses.

## Quick start (local)

### 1. Backend

```powershell
# Option A - offline demo, no database required
$env:SPRING_PROFILES_ACTIVE='h2'
mvn -f backend/pom.xml -B spring-boot:run

# Option B - local Postgres (default profile)
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/yep'
$env:DATABASE_USERNAME='yep'; $env:DATABASE_PASSWORD='yep'
mvn -f backend/pom.xml -B spring-boot:run
```

API base: `http://localhost:8080` - health: `http://localhost:8080/actuator/health`

**Seeded demo accounts** (password `Password123!`): `admin@yep.africa`,
`amara@yep.africa` (facilitator), `brian@yep.africa` (mentor), `sarah@yep.africa`,
`daniel@yep.africa`, `grace@yep.africa`, `mikiyas@yep.africa` (participants).

### 2. Frontend

```powershell
cd frontend
Copy-Item .env.example .env.local     # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev                            # http://localhost:3000
```

### 3. Tests / quality gates

```powershell
# Backend - unit + integration + schema validation (18 tests)
mvn -f backend/pom.xml -B test

# Frontend - type checking + production build
cd frontend
npm run typecheck
npm run build
```

## API map (v1)

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/change-password` |
| Exchanges | `GET /exchanges?level&topic&focusArea&status&q&from&to&page&size`, `GET/PUT /exchanges/{id}`, `POST /exchanges`, `PATCH /exchanges/{id}/status`, `POST /exchanges/{id}/enroll`, `GET /exchanges/{id}/enrollments` |
| Enrolments | `GET /enrollments/mine`, `POST /enrollments/{id}/complete`, `POST /enrollments/{id}/withdraw` |
| Teams | `GET /teams?exchangeId`, `GET/POST /teams`, `GET /teams/{id}`, `POST /teams/{id}/join`, `POST/DELETE /teams/{id}/members[/{participantId}]`, `POST /teams/{id}/mentor`, `GET /teams/{id}/project` |
| Projects | `GET /projects?status&mine`, `GET/PUT /projects/{id}`, `POST /projects?teamId=`, `POST /projects/{id}/submit|review|complete` |
| Assessments | `GET /assessments?participantId&teamId&mine`, `GET /assessments/about-me`, `GET /assessments/{id}`, `POST /assessments` |
| Certificates | `GET /certificates/mine`, `GET /certificates`, `GET /certificates/verify/{code}`, `POST /certificates` |
| Messages | `GET/POST /messages`, `POST /messages/{id}/read`, `GET/POST /messages/announcements`, `POST /messages/announcements/{id}/approve`, `GET /messages/pending-approval`, `GET /messages/unread-count` |
| Users (admin) | `GET /users?role&q&page&size`, `GET/PATCH/PUT /users/{id}`, `POST /users` |
| AI | `POST /ai/suggest`, `POST /ai/moderate` |

Errors always come back as
`{ timestamp, status, error, code, message, path, fieldErrors }` (see `GlobalExceptionHandler`).

## OOP in the codebase

The domain model follows the four pillars explicitly - see
[docs/UML_CLASS_DIAGRAM.md](docs/UML_CLASS_DIAGRAM.md) for the diagram and the mapping table:
encapsulation (private state + intent-revealing methods), inheritance (three `SINGLE_TABLE`
hierarchies over `users`, `virtual_exchanges`, `messages`), polymorphism (`Assessable`,
`Certifiable`, `Notifiable`, abstract `VirtualExchange` behaviour) and abstraction (interfaces,
DTO contracts, `PageResponse<T>`).

## Deployment

Neon (Postgres) -> Render (Dockerised API, Flyway on boot) -> Vercel (Next.js). Full steps,
environment variables, health checks and troubleshooting:
[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Licence / contact

Built for the YEP programme. No data is sold or shared with advertisers; registration requires
explicit consent and only the fields needed to run the programme are stored.
