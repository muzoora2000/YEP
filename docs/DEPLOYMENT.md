# YEP - Deployment Guide (Neon + Render + Vercel)

Three managed services, one build each. Deploy in this order: **Neon -> Render -> Vercel**, because
the API needs its database before the browser client can call it.

---

## 1. Neon (PostgreSQL)

Create the database from the **Vercel Marketplace** or a standalone Neon account. Only the API
connects to PostgreSQL; the Next.js client never does.

### Option A - Vercel Marketplace

1. Vercel Dashboard -> **Storage** -> **Create Database** -> **Neon** -> **Install**.
2. Choose **Create New Neon Account**, pick a region matching the Render service and the free plan.
3. Copy the **direct / unpooled** connection string (`DATABASE_URL_UNPOOLED`), for example:
   `postgresql://user:password@ep-xxxx.aws.neon.tech/yep?sslmode=require`.
4. Pass it to Render as `DB_URL`.

### Option B - Existing Neon account

From the Neon project dashboard choose **Connect -> Direct** (not pooled) and use that string as
`DB_URL`.

Why **direct**: Flyway runs on boot over JDBC. The pooled PgBouncer endpoint uses transaction
pooling and can break migration connections.

### Schema management

The `prod` profile runs Flyway from `classpath:db/migration` and Hibernate uses
`ddl-auto=validate`, so the schema is only changed by migrations:

- `V1__core_from_er.sql` - core ER model
- `V2__additions.sql` - added columns and tables
- `V3__indexes.sql` - indexes

To change the schema, add `V4__your_change.sql`, redeploy and let Flyway apply it on boot.

---

## 2. Render (Spring Boot API)

Create a **Web Service** from the repository.

| Setting | Value |
|---|---|
| Root directory | `backend` |
| Runtime | `Docker` (uses `backend/Dockerfile`) or native Maven build |
| Build command | `mvn -B clean package` (native) |
| Start command | `java -jar target/yep-backend-*.jar` (native) |
| Health check path | `/actuator/health` |

### Environment variables

| Variable | Value | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Flyway + `ddl-auto: validate` |
| `DB_URL` | Neon direct JDBC URL | `jdbc:postgresql://...` |
| `DB_USER` | Neon user | optional if embedded in URL |
| `DB_PASSWORD` | Neon password | optional if embedded in URL |
| `DB_DIRECT_URL` | Neon direct JDBC URL | optional; falls back to `DB_URL` |
| `JWT_SECRET` | 32+ random characters | never commit this |
| `JWT_EXPIRY_HOURS` | `8` | optional |
| `FRONTEND_URL` | `https://your-app.vercel.app` | used for certificate QR links |
| `ALLOWED_ORIGINS` | `https://your-app.vercel.app` | comma-separated CORS allow-list |
| `ADMIN_EMAIL` | admin email | seeds the first administrator when `users` is empty |
| `ADMIN_PASSWORD` | strong password | seeds the first administrator |
| `ADMIN_NAME` | `YEP Administrator` | optional |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP details | optional; used for email notifications |
| `AT_USERNAME`, `AT_API_KEY` | Africa's Talking credentials | optional; used for SMS notifications |
| `PORT` | provided by Render | read via `${PORT:8080}` |

Notes:

- The `prod` profile marks the auth cookie `Secure; HttpOnly`; Render and Vercel serve HTTPS.
- The first deploy log should show Flyway applying migrations `1`, `2` and `3`.
- Scale-to-zero free instances can take ~30s to wake on the first request.

### Deploying a change

1. Push to `main` or trigger a manual deploy.
2. Render builds the Docker image (`mvn -DskipTests package`) and starts the fat jar.
3. The health check must return `{"status":"UP"}` before traffic is routed.

---

## 3. Vercel (Next.js client)

1. **Add new -> Project -> Import Git Repository**, framework preset **Next.js**.
2. Root directory: `frontend`; build command `npm run build`; output `.next`.
3. Environment variables:

   | Variable | Value |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://yep-backend.onrender.com` |
   | `API_PROXY_TARGET` | optional override for the proxy target |
   | `NEXT_PUBLIC_APP_URL` | `https://your-app.vercel.app` |

4. Deploy. Browser calls go to the same-origin Next.js proxy at `/api/*`, which forwards
   server-side to `NEXT_PUBLIC_API_URL` (or `API_PROXY_TARGET`). This avoids CORS and keeps the
   JWT cookie on one origin.

### Post-deploy checklist

- [ ] `https://yep-backend.onrender.com/actuator/health` returns `{"status":"UP"}`
- [ ] `GET https://yep-backend.onrender.com/api/v1/activities?page=0&size=5` returns JSON
- [ ] Open the Vercel app, register a participant and log in
- [ ] Approve a facilitator/mentor/partner account from `/admin`
- [ ] Create and publish an activity, enroll a participant and check in to a session
- [ ] Submit and grade an assessment, then issue a certificate
- [ ] Open `/certificates/verify`, enter the certificate number and download the PDF
- [ ] Verify the partner opportunity and project showcase flows

---

## Environment reference

| Variable | Where | Required |
|---|---|---|
| `DB_URL` | Render | yes |
| `DB_USER`, `DB_PASSWORD` | Render | optional if in URL |
| `DB_DIRECT_URL` | Render | optional |
| `JWT_SECRET` | Render | yes in production |
| `JWT_EXPIRY_HOURS` | Render | optional (default 8) |
| `FRONTEND_URL` | Render | yes for certificate QR links |
| `ALLOWED_ORIGINS` | Render | yes (frontend origin) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | Render | first boot only |
| `MAIL_*`, `AT_USERNAME`, `AT_API_KEY` | Render | optional |
| `PORT` | Render | injected |
| `SPRING_PROFILES_ACTIVE` | Render | `prod` |
| `NEXT_PUBLIC_API_URL` | Vercel | yes (proxy target) |
| `API_PROXY_TARGET` | Vercel | optional |
| `NEXT_PUBLIC_APP_URL` | Vercel | optional |

---

## Local development

```powershell
# PostgreSQL must be running locally
$env:DB_URL='jdbc:postgresql://localhost:5432/yep'
$env:DB_USER='yep'
$env:DB_PASSWORD='yep'
$env:ADMIN_EMAIL='admin@yep.local'
$env:ADMIN_PASSWORD='ChangeMe123!'

& "$env:TEMP\apache-maven-3.9.9\bin\mvn.cmd" -B -f backend/pom.xml spring-boot:run

# Client
cd frontend
Copy-Item .env.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev                          # http://localhost:3000
```

The first administrator is created only when the `users` table is empty and `ADMIN_EMAIL` /
`ADMIN_PASSWORD` are set. Tests use an in-memory H2 database with PostgreSQL compatibility mode;
the running application uses PostgreSQL.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| CORS error in the browser | browser calls the Render URL directly | use the same-origin `/api` proxy or add the exact origin to `ALLOWED_ORIGINS` |
| `Invalid cookie, expected Secure` | cookie marked `Secure` but served over HTTP | use HTTPS in production; use `dev` locally |
| `Schema-validation: missing table` | Flyway did not run or wrong profile | check `SPRING_PROFILES_ACTIVE=prod`, Flyway logs and `DB_URL` |
| `JWT signature does not match` after redeploy | `JWT_SECRET` changed | set a stable 32+ character secret |
| Login returns 401 | wrong proxy target or API asleep | check `/actuator/health`, then `NEXT_PUBLIC_API_URL` |
| Flyway `Connection refused` / auth failure | pooled endpoint or rotated password | use the direct Neon endpoint and re-copy credentials |
| Render cannot reach Neon | Neon IP allowlist or missing `sslmode` | allow Render egress and keep `?sslmode=require` |
| `relation already exists` | local `ddl-auto=update` residue | drop the local database and let Flyway recreate it |
