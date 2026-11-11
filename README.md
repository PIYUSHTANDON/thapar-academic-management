# Thapar Academic Management System

A Java 21 / Spring Boot application with a React + TypeScript faculty workspace. The integrated Student Dashboard and all eight student modules are implemented from the approved student reference.

## Project structure

```text
backend/
  pom.xml
  mvnw.ps1                     Windows Maven bootstrap (project-local cache)
  .env.example
  src/main/java/com/thapar/ams/
    AmsApplication.java
    config/                    Local-only synthetic seeding
    controller/                Auth, faculty and student REST endpoints
    dto/                       Validated authentication request
    entity/                    14 relational JPA entities
    exception/                 Safe API validation responses
    repository/                Spring Data repositories
    security/                  BCrypt, session security and login throttle
    service/                   Faculty data queries and ownership checks
  src/main/resources/
    application.yml            local H2 / mysql profiles
    db/migration/V1__academic_schema.sql
  src/test/java/com/thapar/ams/AcademicIntegrationTest.java
frontend/
  .env.example
  package.json / package-lock.json
  vite.config.ts
  src/
    assets/                    Actual reference logo and campus image crop
    components/                Panels, calendar and shared UI
    hooks/                     Session authentication context
    layouts/                   Faculty navigation, header and search
    pages/auth/                Role selection and faculty/student sign-in
    pages/faculty/             Dashboard and working faculty modules
    pages/student/             Student Dashboard, modules, QR scanner and shared UI
    routes/                    Role-protected routes
    services/                  Axios client and CSRF handling
    styles.css                 Tailwind + reference-based responsive styling
references/                    Original supplied references, unchanged
scripts/smoke_test.py           Ephemeral real-server HTTP test
```

## Requirements

- JDK **21**, Node.js **22+**, npm, and Maven **3.9+**.
- MySQL **8** is optional for local use. The default `local` profile uses an H2 file database in `data/` relative to the server working directory.
- Phase 2 QA installed a checksum-verified portable Microsoft JDK 21 under `.tools/jdk21/`. The backend was compiled, packaged, started and tested on JDK 21. See [QA_REPORT.md](QA_REPORT.md) for exact results.

## Run locally on Windows

Open two PowerShell terminals in the project root.

### 1. Configure local synthetic accounts and start Java

The application has **no default login credentials**. Choose your own local identifiers and passwords. Passwords must have at least 12 characters. No real faculty/student records are seeded.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SEED_ENABLED = 'true'
$env:SEED_FACULTY_ID = Read-Host 'Choose a local faculty ID'
$env:SEED_STUDENT_ID = Read-Host 'Choose a local student ID'
$facultySecret = Read-Host 'Choose faculty password (12+ characters)' -AsSecureString
$studentSecret = Read-Host 'Choose student password (12+ characters)' -AsSecureString
$env:SEED_FACULTY_PASSWORD = [System.Net.NetworkCredential]::new('', $facultySecret).Password
$env:SEED_STUDENT_PASSWORD = [System.Net.NetworkCredential]::new('', $studentSecret).Password
powershell -ExecutionPolicy Bypass -File backend/mvnw.ps1 spring-boot:run
```

`mvnw.ps1` downloads Maven 3.9.9 from Maven Central into `.tools/` if needed and uses a project-local dependency cache. If Maven is installed, `mvn -f backend/pom.xml spring-boot:run` also works. The `ExecutionPolicy` option applies to this child process only.

Seeding runs only under the `local` profile with `SEED_ENABLED=true`. If the faculty identifier already exists, it leaves existing records untouched. Retain the same IDs on later runs, or turn seeding off after the first successful start. Changing seed environment passwords does not reset existing accounts. The sample includes a named demo faculty, one explicitly synthetic student, three courses, today's sessions, draft assessments, a submission, a message, a notification and an academic event.

### 2. Start React

```powershell
npm.cmd install --prefix frontend
npm.cmd run dev --prefix frontend
```

Visit **http://127.0.0.1:5173**. Choose Faculty or Student, then use the credentials you configured. Java listens on port 8080; Vite proxies `/api` to it. Use the same hostname throughout. No CORS bypass is needed.

The `.env.example` files list available settings. Spring Boot does **not** automatically load a `.env` file: export backend values in the process environment or configure them in your IDE. Vite loads `frontend/.env` if you create it.

## MySQL 8

Create an empty `thapar_ams` database and a dedicated application database user with migration privileges. Configure:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://localhost:3306/thapar_ams'
$env:DB_USER = Read-Host 'Database user'
$dbSecret = Read-Host 'Database password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbSecret).Password
$env:SEED_ENABLED = 'false'
powershell -ExecutionPolicy Bypass -File backend/mvnw.ps1 spring-boot:run
```

Flyway creates the schema and Hibernate validates it. The MySQL profile requires HTTPS cookies, so serve the frontend and `/api` through the same HTTPS origin behind a correctly configured reverse proxy. MySQL was not available in this environment and has not been runtime-tested. There is no production account-provisioning UI; provision approved users and their faculty/student relationships through an administrative process with BCrypt hashes. Never enable local demo seeding for production.

## Implemented behavior

- Role selection; reference-inspired faculty login; branded basic student login; password visibility, validation, error handling and session restoration.
- Faculty dashboard uses server-derived statistics, today's teaching schedule, month navigation, selected dates, backend events, quick links and assessment actions.
- Collapsible desktop sidebar and mobile drawer; all eight modules have routes. Header search queries authorized courses/students; notifications can be marked read; profile menu and logout work.
- Messages: enrolled student conversations, sender/receiver labels, persisted REST history and sending.
- Courses: assigned courses, authorized student roster, editable title/description, resource uploads/downloads, persisted announcements and student notification creation.
- Assessments: assignments/quizzes with deadlines, submission list, marks from 0–100, result publication. This is a basic assessment workflow, not a full quiz-question/exam engine.
- Calendar: classes, assessment deadlines, custom academic events and clickable dates.
- Attendance: assigned class sessions, QR generated locally (no token sent to a third party), 3-minute random tokens stored as SHA-256 hashes, enrollment/identity checks and unique attendance constraints. Sessions open 15 minutes before class and close at class end. A new token invalidates the previous one. Duplicate attendance receives HTTP 409.
- Settings: permitted name changes, notification preference storage, BCrypt password changes and session invalidation.
- Student login opens `/student/dashboard`. Students can use their enrolled courses, assessments, submissions, published results, attendance QR/image scanning, messages, calendar and profile settings.

## Authentication and authorization

`POST /api/v1/auth/login` authenticates a validated identifier/password/role. Spring Security persists authentication in a server session with HttpOnly, SameSite=Strict cookies; HTTPS deployments must use Secure cookies. Session IDs rotate on login. The default timeout is 30 minutes; Remember Me extends the current session inactivity timeout to 8 hours. It is not a persistent cross-browser remember-me credential.

The frontend obtains a session CSRF token from `GET /api/v1/auth/csrf`, sends it in the returned header for mutations, and refreshes it after login/logout. `POST /api/v1/auth/logout` invalidates the session and deletes the cookie. `GET /api/v1/auth/session` returns a safe signed-in status and the current user without producing a 401 during normal anonymous startup. `GET /api/v1/auth/me` remains protected. Role checks are enforced on the server; faculty course ownership is checked before accessing resources or student data. Entity graphs and password hashes are not serialized to API responses.

Login is limited to ten attempts per source IP in a 15-minute in-memory window. The limiter is suitable for a single local application instance; distributed deployments need shared rate limiting and trusted proxy configuration. Password changes invalidate the current session; revocation of other concurrent sessions is a future production enhancement.

The forgot-password control explains the institute helpdesk recovery path. Email recovery and institute SSO are not configured; no simulated SSO button or reset email is presented.

## API surface

All endpoints are prefixed with `/api/v1`.

| Area | Endpoints |
| --- | --- |
| Authentication | `GET /auth/csrf`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout` |
| Dashboard | `GET /faculty/dashboard`, `/faculty/schedule`, `/faculty/notifications` |
| Courses | `GET /faculty/courses`, `PUT /faculty/courses/{id}` |
| Resources | `GET/POST /faculty/courses/{id}/resources`, `GET /faculty/resources/{id}` |
| Announcements | `POST /faculty/courses/{id}/announcements` |
| Roster | `GET /faculty/students` (only assigned-course enrollments) |
| Messages | `GET/POST /faculty/messages` |
| Attendance | `GET /faculty/attendance`, `POST /faculty/attendance/sessions` |
| Student claim | `POST /student/attendance/claim` with `{ "token": "QR token" }`; authenticated enrolled student only |
| Assessments | `GET/POST /faculty/assessments`, `POST /faculty/assessments/{id}/publish` |
| Grading | `GET /faculty/submissions`, `POST /faculty/submissions/{id}/grade` |
| Calendar | `GET/POST /faculty/calendar` |
| Settings | `PUT /faculty/settings/profile`, `PUT /faculty/settings/password` |
| Notification | `POST /faculty/notifications/{id}/read` |

The initial implementation fetches small authorized lists for search/filtering. Pagination and query-side search should precede large deployments.

## Database

The Flyway migration defines `User`, `Faculty`, `Student`, `Course`, `Enrollment`, `ClassSession`, `AttendanceRecord`, `Assessment`, `Submission`, `Grade`, `Message`, `Notification`, `CalendarEvent` and `LearningResource`. Foreign keys enforce relationships; unique constraints cover user identifiers, course codes, enrollment numbers, student/course enrollments, student/session attendance and student/assessment submissions. Learning-resource files are stored in the database; multipart requests are limited to 10 MB. Production storage, file validation and malware scanning need a separate deployment design.

## Build and verify

```powershell
powershell -ExecutionPolicy Bypass -File backend/mvnw.ps1 test
powershell -ExecutionPolicy Bypass -File backend/mvnw.ps1 package
npm.cmd run build --prefix frontend
python scripts/smoke_test.py
```

The HTTP smoke test launches an isolated Java server on port 8081 with a temporary in-memory H2 database and random environment-provided accounts. It tests real cookie/CSRF login, faculty APIs, persisted writes and student role denial, then terminates its server. It does not modify your local database. Python 3 is needed only for this optional check.

Executed Phase 2 results and browser screenshots are documented in [QA_REPORT.md](QA_REPORT.md). The browser suite uses real Spring Boot APIs and isolated file-backed H2 databases with random, process-environment credentials.

JUnit covers login, invalid credentials/role, CSRF enforcement and rotation, logout, role denial, course-level isolation, messaging persistence and attendance replay rejection. `backend/target/surefire-reports/` contains executed results. Frontend production output is `frontend/dist/`; the executable server is `backend/target/ams-1.0.0.jar`.

## Visual reference and remaining work

Both supplied screenshots and the actual supplied WebP logo were inspected. The interface is recreated from HTML/CSS components; it does not use a reference screenshot as the page background. Because there was no separate campus photograph, `src/assets/campus.jpg` extracts only the unobstructed architectural photo region of the dashboard reference. The login also uses a photo-only crop from the login reference (`campus-login.jpg`). These crops affect framing compared with the original reference; a standalone campus image and a transparent logo/architectural illustration would improve fidelity. No unrelated imagery was substituted.

Playwright Chromium browser verification and JDK 21 execution are covered by Phase 2 QA. MySQL 8 remains unverified because no local MySQL service was available and Docker Engine was not running. See [QA_REPORT.md](QA_REPORT.md) for tested viewports, defects, fixes, screenshots and remaining visual differences.

Further work: GPS/proximity checks, full quiz authoring, self-service recovery, configured SSO, pagination, production account administration, distributed session/rate-limit infrastructure, audit logging, deployment and backups. QR possession alone is not proof of physical presence; identity/enrollment checks are implemented and GPS/proximity is deliberately deferred.

Security implementation follows Spring Security's [session](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html) and [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) guidance.

## Browser QA

Build the backend first. Keep the main app running at **http://127.0.0.1:5173/** so the suite can verify that exact URL. Then run:

```powershell
$env:PLAYWRIGHT_BROWSERS_PATH = "$PWD/.tools/playwright"
frontend/node_modules/.bin/playwright.cmd install chromium
npm.cmd run test:e2e --prefix frontend
```

The suite starts and stops its own Vite server on **5174** and Java server on **8082**, generates random credentials in process memory, and writes isolated databases under `.tools/qa/`. It never changes the main `data/ams` database. It creates an active attendance session through a guarded test-only JDBC fixture so QR attendance can be tested at any time of day. The runner preserves the production login throttle and restarts between viewport projects rather than disabling security. After each project, it saves a canary event, restarts Java, and verifies the event survived. The Faculty regression run also verifies the actual student login/logout forms at all four sizes; the dedicated Phase 3 Student suite covers the complete portal.

`QA_JAVA_HOME` can select a JDK; otherwise the runner uses the portable JDK 21 when present, then the system Java. The JDBC fixture expects H2 in the Maven wrapper's project-local cache. Main app ports **5173/8080** and QA ports **5174/8082** must be available as described. For a single viewport, set `$env:QA_PROJECT = 'mobile'` (or `desktop`, `laptop`, `tablet`), and unset it for the full matrix. `node scripts/verify-student-browser.mjs` can run the supplemental student-form checks alone.

Reports are written to `qa/results/`, HTML reports to `qa/html/`, and screenshots to `qa/screenshots/`. Traces, videos and storage-state files are disabled to avoid persisting authentication secrets. Test credentials are not hardcoded in source, including the Java integration tests. The local H2 profile now uses synchronous writes (`WRITE_DELAY=0`) so a newly committed record survives abrupt Java termination; MySQL configuration is unchanged.

## Reusable local Faculty demo account

The optional **faculty-demo** mode uses the existing Spring seed runner. It is restricted to the sole `local` profile with an H2 datasource. New demo users are marked `local_only`; authentication rejects those users under MySQL, staging, production, mixed profiles or non-H2 databases, even if a demo record is copied into another database.

For the requested local account, stop the project backend on port 8080, then run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local-faculty-demo.ps1
```

The script defaults to Faculty ID **FACULTY001** and display name **Demo Faculty**. It securely prompts for the disposable local password; no password default is embedded in the script, source or tracked configuration. The password is passed through the process environment and stored only as a BCrypt hash in H2. On subsequent starts, an existing account's role, name and password must match; otherwise startup reports a conflict without overwriting it. Repeated runs preserve the existing hash and avoid duplicate synthetic course records. Unrelated accounts and courses are not rewritten.

Demo-owned examples include three courses, today's teaching sessions, a historical session with synthetic attendance, draft assessments and a sample submission. The synthetic student has a randomly generated, undisclosed password and is also local-only. Once provisioned, the Faculty account persists in `data/ams.mv.db` and works with a normal local backend startup without seeding enabled. Keep using the `local` profile; never migrate this disposable local database into a deployed environment.

Login: **http://127.0.0.1:5173/login/faculty**. To rerun its real API/Playwright verification, provide `LOCAL_DEMO_PASSWORD` in the process environment and run `node scripts/verify-local-faculty-demo.mjs`. Results are written to `qa/results/local-faculty-demo.json` without credentials; the dashboard screenshot is in `qa/screenshots/local-faculty-demo-dashboard.png`.

## Phase 3: integrated Student Portal

- Student login: **http://127.0.0.1:5173/login/student**
- Student dashboard: **http://127.0.0.1:5173/student/dashboard** (authenticated students only)
- The former `/student/pending` URL redirects to the dashboard.

The approved student design is recreated using components and scoped styling. Quick Actions contains exactly three controls: **View Timetable**, **Submit Assignment**, and **Check Results**. Student name, enrollment, academic year, semester, attendance, courses and assessment status come from the Java backend. Official CGPA is nullable and displays **Not available** until an authorized academic record supplies it; assignment marks never generate CGPA.

All eight Student sections are implemented. Messages, course materials, announcements, submissions, published grades/feedback, attendance claims and class information use the same entities and database as the Faculty Portal. Faculty assessments now support submission attachment downloads and feedback; the Faculty Calendar includes a class-information editor. The approved Faculty Dashboard layout was not rebuilt.

Student submission uploads support **PDF** (checked PDF signature) and **UTF-8 TXT**, up to **5 MB**, stored in the database. Downloads require the submitting student or the owning course faculty and are served as attachments with `nosniff`. Deadline, duplicate-submission and enrollment checks run server-side. Submitted work is not editable/resubmittable in this initial workflow. PDF malware scanning and a full quiz-question/exam engine are not implemented.

Attendance supports short-lived Faculty QR codes through camera scanning, local QR-image decoding and manual token entry. The server validates session state, expiry, authenticated student identity and enrollment, and rejects duplicate claims. Camera hardware availability/permissions depend on the browser; QR-image decoding and manual fallback are independently available. **GPS/proximity verification is not implemented.**

### Create a local-only Student demo

The existing `FACULTY001` local demo and its synthetic courses can be reused. Choose your own Student password; there is no committed/default Student password.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local-student-demo.ps1 -StudentId STUDENT001 -DisplayName 'Demo Student' -FacultyId FACULTY001 -Restart
```

The script securely prompts for a password of at least 12 characters. `-Restart` stops port 8080 only when it can identify the Java process as this project's executable JAR; it refuses to stop an unrecognized process. Omit the switch if the backend is already stopped. The script starts the backend on port 8080 with `local`, H2 and the opt-in `student-demo` seed mode. The Faculty demo must already exist and be marked local-only, with synthetic courses. Existing matching student accounts are verified, not reset; mismatched credentials/name/role cause a conflict and no overwrite.

The new account is BCrypt-hashed, local-only, visibly synthetic and enrolled in those demo courses. Existing Faculty credentials, students, enrollments and coursework are preserved. Official CGPA remains unset. The account persists in `data/ams.mv.db` and works on ordinary local starts with seeding disabled. The local-only authentication restriction also applies if records are copied to a non-local profile/database.

### Student APIs

All routes use `/api/v1/student` and the same HttpOnly session/CSRF architecture.

| Function | Routes |
| --- | --- |
| Dashboard/profile | `GET /dashboard`, `GET /profile` |
| Enrolled courses | `GET /courses`, `GET /courses/{id}/resources`, `GET /resources/{id}` |
| Schedule/calendar | `GET /schedule`, `GET /calendar` |
| Assessments/submissions | `GET /assessments`, `GET /submissions`, `POST /submissions` (multipart), `GET /submissions/{id}/file` |
| Results | `GET /results` (published marks/feedback only, plus stored official CGPA) |
| Messages | `GET /messages/contacts`, `GET /messages`, `POST /messages` |
| Attendance | `GET /attendance`, `POST /attendance/claim` |
| Announcements/notifications | `GET /announcements`, `GET /notifications`, `POST /notifications/{id}/read` |
| Search/settings | `GET /search?q=...`, `PUT /settings/profile`, `PUT /settings/password` |

Student identity is always derived from the authenticated principal. Active enrollment is required for course data, downloads and attendance. Notification/read and submission/file routes additionally validate ownership. Official academic fields are not writable by Student profile endpoints. Grading remains hidden until both the assessment results and the grade are published. Updating a published mark returns it to an unpublished state until Faculty chooses **Publish updates**.

### Phase 3 testing

```powershell
npm.cmd run test:student --prefix frontend
$env:QA_REPORT_TAG = 'faculty-phase3'
node scripts/run-browser-qa.mjs
Remove-Item Env:QA_REPORT_TAG
npm.cmd run build --prefix frontend
```

Student and Faculty suites run in separate isolated servers/databases so the production login throttle remains enabled. Set `QA_PROJECT` to `desktop`, `laptop`, `tablet` or `mobile` for one viewport; unset it for the full matrix. Student report files are `qa/results/*-student-phase3.json`, screenshots are `qa/screenshots/student-*-dashboard.png`, and full execution results and limitations are in [STUDENT_QA_REPORT.md](STUDENT_QA_REPORT.md).

Flyway **V3** adds nullable official student metadata, active enrollment state, announcement description/publication time, submission attachment fields and grade feedback. It does not delete or reset existing data. A private pre-migration copy of the main local database was retained under `.tools/phase3-backup/`.
