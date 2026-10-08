# YEP - Deployment Guide (Neon + Render + Vercel)

Three managed services, one build each. Deploy in this order: **Neon -> Render -> Vercel**,
because the API needs its database and origin allow-list before the browser client can call it.

---

## 1. Neon (PostgreSQL)

New users can no longer sign up at neon.tech directly - create the database from the **Vercel
Marketplace**, then hand the connection string to Render. The database is only ever used by the
API, never by the Next.js client.

### Option A - Vercel Marketplace (recommended)

1. Vercel Dashboard -> **Storage** -> **Create Database** (Marketplace -> **Neon** -> **Install**).
2. Choose **"Create New Neon Account"** -> accept terms -> pick a **region matching your Render
   service** (e.g. both Frankfurt, both Oregon) and the **Free plan** -> name it `yep`.
   - Billing goes through Vercel; a Neon account is created under a `Vercel: <team>` organization.
   - When asked which Vercel project to connect, pick your frontend project. Vercel automatically
     injects `DATABASE_URL` / `DATABASE_URL_UNPOOLED` into *Vercel's* environment - harmless here,
     our client never talks to the database. Ignore them.
3. On the Storage tab -> connection details -> copy the **direct / unpooled** string
   (`DATABASE_URL_UNPOOLED`), or click **Open in Neon** -> **Connect** -> select the **Direct**
   endpoint. It looks like:
   `postgresql://user:password@ep-xxxx.aws.neon.tech/yep_db?sslmode=require`
4. Pass it to Render as `DATABASE_URL`.

### Option B - Existing standalone Neon account

1. From your Neon project dashboard, **Connect -> Direct** endpoint (not pooled).
2. Same hand-off: `DATABASE_URL` on Render.

Why **direct**: Flyway runs on boot over JDBC - the pooled (pgbouncer) endpoint uses
transaction pooling, which breaks migration connections.

### Preview branches

The integration creates a `preview/<git-branch>` database branch per Vercel preview deployment.
Only Render connects to the main database, so these branches are inert for this project - you can
leave automated preview branching on and ignore it.

Schema management:
- `prod` profile runs **Flyway** from `classpath:db/migration` (`V1__init_schema.sql`) with
  `spring.jpa.hibernate.ddl-auto=validate`, so the database is only ever changed by a migration file.
- To change the schema, add `V2__your_change.sql`, redeploy, Flyway applies it on boot.

Schema management:
- `prod` profile runs **Flyway** from `classpath:db/migration` (`V1__init_schema.sql`) with
  `spring.jpa.hibernate.ddl-auto=validate`, so the database is only ever changed by a migration file.
- To change the schema, add `V2__your_change.sql`, redeploy, Flyway applies it on boot.

---

## 2. Render (Spring Boot API)

**New -> Web Service** from the repository (or Docker-only repo):

| Setting | Value |
|---|---|
| Root directory | `backend` |
| Runtime | `Docker` (uses `backend/Dockerfile`) or `Native` with build `mvn -B clean package` and start `java -jar target/yep-backend-*.jar` |
| Health check path | `/actuator/health` |
| Instance type | Starter is enough for a demo |

### Environment (Render -> Environment)

| Variable | Value | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | turns on Flyway + `ddl-auto: validate` |
| `DATABASE_URL` | Neon connection string | `postgres://...` is auto-rewritten to a JDBC URL by `PostgresUrlEnvironmentPostProcessor` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Neon user/password | only needed if they are not embedded in the URL |
| `JWT_SECRET` | 32+ random characters | e.g. `openssl rand -base64 48` - **never commit this** |
| `ALLOWED_ORIGINS` | `https://your-app.vercel.app` | comma separated; this is the CORS allow-list |
| `DB_POOL_SIZE` | `10` (optional) | Hikari pool size |
| `PORT` | provided by Render | already read via `${PORT:8080}` |

Notes:
- `yep.cookie.secure=true` in `prod`, so the auth cookie is `Secure; HttpOnly` - correct because
  Render/Vercel both serve HTTPS.
- After the first deploy the log should show `Migrating schema ... to version "1"` then
  `Successfully applied 1 migration`. Subsequent boots log `Schema is up to date`.
- Scale-to-zero free instances: the first request after idle can take ~30s (cold start).

### Deploying a change

1. Push to `main` (or use **Manual Deploy**).
2. Render builds the Docker image: `mvn -DskipTests package` inside the container,
   then starts the fat jar as a non-root user.
3. Health check must return `{"status":"UP"}` before traffic is routed.

---

## 3. Vercel (Next.js client)

1. **Add new -> Project -> Import Git Repository**, framework preset **Next.js**.
2. Settings -> General:
   - **Root directory**: `frontend`
   - Build command `npm run build`, output `.next` (defaults are fine)
3. Settings -> Environment Variables:

   | Variable | Value |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://yep-backend.onrender.com` |
   | `NEXT_PUBLIC_APP_URL` | `https://your-app.vercel.app` |

4. Deploy. Preview deployments get their own URL - add it to `ALLOWED_ORIGINS` on Render if you
   want preview builds to log in.

### Post-deploy checklist

- [ ] `https://yep-backend.onrender.com/actuator/health` returns `{"status":"UP"}`
- [ ] `GET https://yep-backend.onrender.com/api/v1/exchanges?page=0&size=5` returns JSON
- [ ] Open `https://your-app.vercel.app` -> **Register** a participant -> confirm the account
- [ ] Log out, log in again, check the dashboard loads enrolments/teams
- [ ] `https://your-app.vercel.app/certificates/verify` accepts a code from the DB
- [ ] Install as PWA (manifest + `sw.js` served from `/manifest.webmanifest`, `/sw.js`)

---

## Environment reference

| Variable | Where | Required |
|---|---|---|
| `DATABASE_URL` | Render | prod |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Render | optional |
| `JWT_SECRET` | Render | recommended (32+ chars) |
| `JWT_EXPIRATION_MS` | Render | optional (default 86400000 = 24h) |
| `ALLOWED_ORIGINS` | Render | prod (frontend origin(s)) |
| `DB_POOL_SIZE` | Render | optional |
| `PORT` | Render | injected |
| `SPRING_PROFILES_ACTIVE` | Render | `prod` |
| `NEXT_PUBLIC_API_URL` | Vercel | always |
| `NEXT_PUBLIC_APP_URL` | Vercel | optional |

---

## Local development / offline demo

```powershell
# API - offline demo (in-memory H2, no Postgres needed)
$env:SPRING_PROFILES_ACTIVE='h2'
mvn -f backend/pom.xml -B spring-boot:run

# API - full stack against a local Postgres
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/yep'
$env:DATABASE_USERNAME='yep'; $env:DATABASE_PASSWORD='yep'
mvn -f backend/pom.xml -B spring-boot:run

# Client
cd frontend
Copy-Item .env.example .env.local   # NEXT_PUBLIC_API_URL=http://localhost:8080
npm install
npm run dev                          # http://localhost:3000
```

Seeded demo accounts (created by the demo seeder): password `Password123!` - check the backend log
for the generated emails when the `h2`/`dev` profile starts.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| CORS error in the browser | `ALLOWED_ORIGINS` does not include the Vercel origin (or a trailing `/`) | set the exact origin, e.g. `https://my-app.vercel.app` |
| `Invalid cookie, expected Secure` | cookie marked `Secure` but served over plain HTTP | keep `prod` on HTTPS only; use `dev`/`h2` locally |
| `Schema-validation: missing table` | profile is `prod` but Flyway did not run | check `spring.flyway.enabled=true` and the migration log lines |
| `JWT signature does not match` after redeploy | `JWT_SECRET` changed / not set | set a stable 32+ char secret as a Render environment variable |
| Login returns 401 | wrong `NEXT_PUBLIC_API_URL` or API asleep | verify the health endpoint, then `npm run dev` again |
| Flyway fails `Connection refused` / `password authentication failed` | pasted the **pooled** (pgbouncer) endpoint, or DB password rotated | use the **direct** endpoint (`DATABASE_URL_UNPOOLED`), copy it again from the Storage tab |
| Render cannot reach Neon at all | Neon **IP allowlist** / `sslmode` | Neon project -> Settings -> IP Allowlist: add `0.0.0.0/0` (or Render's egress IPs); keep `?sslmode=require` |
| Stale `DATABASE_URL` in Vercel | Vercel injects its own value on preview branches | ignore it - only the Render env var matters |
| `relation already exists` | local `ddl-auto: update` residue against a migrated DB | drop the local database or run with `SPRING_PROFILES_ACTIVE=h2` |
