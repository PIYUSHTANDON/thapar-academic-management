# JavaFX desktop migration plan

## Goal and boundaries

Replace the React/TypeScript browser client with a JavaFX desktop client. Keep Java as the primary implementation language, use JavaFX CSS and the existing image assets for presentation, and preserve the current faculty and student workflows. The existing Spring Boot API, Spring Security rules, Flyway migrations, and database remain the source of truth. Treat the current frontend and QA screenshots as a behavior and visual reference until the desktop replacement reaches parity.

The current web CSS is a design reference, not drop-in JavaFX CSS. Match its palette, typography, dimensions, spacing, states, and imagery in a small JavaFX theme. Desktop window resizing replaces mobile browser breakpoints; define supported minimum and normal window sizes before judging visual parity.

## Codebase audit for the migration

| Area | Current source | Migration implication |
| --- | --- | --- |
| Routes and roles | `frontend/src/routes/App.tsx`, `hooks/useAuth.tsx` | Replace route guards with a typed screen navigator and role checks. Keep server authorization authoritative. |
| Shared shell | `layouts/FacultyLayout.tsx`, `layouts/StudentLayout.tsx`, `components/ui/sidebar.tsx` | One shell component with explicit role navigation; preserve collapsed and expanded sidebar, profile menu, alerts, and sign-out. |
| Visual system | `styles.css` (~3,061 lines), `student.css` (~761 lines), `assets/`, `references/`, `qa/screenshots/` | Extract colors, spacing, font sizes, card/table patterns, and assets. Do not carry over accumulated browser overrides. |
| Faculty screens | `pages/faculty/Dashboard.tsx`, `Modules.tsx`, `components/FacultyScheduleEditor.tsx` | Split the 863-line multi-module file into named screens for messages, attendance/reports, courses/roster, assessments, calendar/schedule, and settings. |
| Student screens | `pages/student/StudentDashboard.tsx`, `StudentModules.tsx`, `QrScanner.tsx`, `student-ui.tsx` | Split the 725-line multi-module file into named screens. Preserve submission, result, announcement, and QR states. |
| HTTP/auth | `services/api.ts`, `backend/.../AuthController.java`, `security/SecurityConfig.java` | One Java HTTP client with cookies, CSRF token/header refresh, typed errors, and session restoration. Password change invalidates the session. |
| Business API | `backend/.../FacultyController.java`, `StudentController.java`; `service/FacultyService.java`, `StudentService.java`, `SubmissionService.java` | Reuse the protected API. Its controller records and map responses need explicit Java client DTOs; avoid duplicating business rules in JavaFX views. |
| Persistence | `backend/src/main/resources/db/migration/V1...V3`, entities and repositories | No database rewrite is required for the client migration. Current local profile uses H2; MySQL is a separate deployment concern. |
| QA and launch | `frontend/e2e/`, `qa/`, `scripts/`, `README.md` | Use existing reports/screenshots as baseline. Replace browser-specific launch and QA scripts only after desktop parity. Preserve backend integration coverage. |

The backend also has large feature-mixed classes (`FacultyController`, `StudentController`, `FacultyService`, `StudentService`) and repository accessors in `FacultyService`. These are navigation concerns, but splitting them before the desktop client uses the main workflows would combine migration risk with API changes. Refactor them by feature in phase 6, keeping endpoint paths and JSON contracts stable.

### Contracts that must survive

- Login and session: `GET /api/v1/auth/csrf`, `POST /auth/login`, `GET /auth/session`, `POST /auth/logout`. Use a Java `HttpClient` with a cookie store; attach the current CSRF header to mutations. Refresh CSRF after login, logout, and session invalidation. Handle 401, 403, 409, and 429 distinctly.
- Attendance: faculty `POST /faculty/attendance/sessions` returns a token, session ID, and expiry. The QR currently contains `type`, `token`, and `sessionId` JSON. Student `POST /student/attendance/claim` sends the token. The server enforces faculty ownership, enrollment, expiry, and duplicate claims. Tokens expire after three minutes; interpret server local timestamps in `Asia/Kolkata` and never make the client countdown the authority.
- Files: faculty course resources and student submissions use multipart uploads and protected downloads. Preserve type and size validation, authenticated downloads, and the current final-submission behavior.
- Assessment: faculty creation, grading, publication, student submission, and published results are separate states. Do not infer published marks from a local UI value.
- Messaging, notifications, calendar, schedule, profile, and password changes use existing role-specific endpoints. Keep all writes on the server.

## Target layout

```text
backend/                         existing Spring Boot module
desktop/                         JavaFX Maven module
  src/main/java/com/thapar/ams/desktop/
    App.java                     application startup
    navigation/Screen.java       named destinations and role permissions
    navigation/Navigator.java
    api/ApiClient.java           cookies, CSRF, errors, HTTP primitives
    api/AuthApi.java
    api/FacultyApi.java
    api/StudentApi.java
    model/                       typed request/response records
    ui/common/AppShell.java      sidebar, header, alerts, shared controls
    ui/common/CalendarView.java
    ui/faculty/                  one View class per feature
    ui/student/                  one View class per feature
    qr/QrCodec.java              QR payload encoding/decoding
    qr/QrScanner.java            small scanner interface
  src/main/resources/
    css/                         tokens, shell, common, feature styles
    images/                      approved existing assets
```

Keep JavaFX views responsible for layout and user events; API classes handle transport; records represent responses; server services enforce business rules. Use direct constructors and a small number of shared controls. Avoid a generic page builder, a large `ModulesView`, one universal state object, and raw `Map<String,Object>` throughout the client. A file should normally answer one searchable question: where a screen renders, where its API request is made, or where its style lives.

## Priority order

The small foundation is prerequisite work. After it, tackle workflows with the highest integration and state risk before straightforward data displays. Each phase ends with a runnable checkpoint suitable for switching models.

| Phase | Priority and work | Done when |
| --- | --- | --- |
| 0. Baseline and contract ledger | List each screen, action, endpoint, response fields, error state, and visual reference. Record desktop target window sizes and current behavior, including expected differences from the website. | `docs/desktop-route-map.md` and a screen/endpoint checklist exist; no feature is missing from the inventory. |
| 1. Platform and visual proof | Create the JavaFX module and launch path. Implement `ApiClient` cookies/CSRF/session, login/role selection, named destinations, common shell, theme tokens, fonts/assets, a representative dashboard card/table/sidebar, and background loading without freezing the JavaFX thread. | Both roles can sign in, navigate, sign out, and see a styled desktop shell. The client checks for an existing session at startup; its session cookie remains process-scoped. Server role denial still works. The sample screen follows the reference at agreed window sizes. |
| 2. QR attendance and scanner boundary | Implement faculty session selection, QR generation, expiry display, attendance records/reports; student manual token, QR-image decode, live scanner adapter, claim result. Start with a Java QR codec (ZXing). Prototype the camera path early. If a Java camera adapter proves awkward, implement an isolated helper in the preferred stack that returns only the decoded payload; Java owns the claim request. | Faculty can display a fresh QR; student can claim through manual, image, and camera flows; expiry, duplicate, wrong-role, and unenrolled states are clear. The helper can be replaced through `QrScanner` without touching attendance views. |
| 3. Assessments and submissions | Faculty assessment creation, submission review, grade and feedback, publication; student assessment list, text/PDF/TXT multipart submission, protected download, published result. | The full faculty-to-student lifecycle works, with the same deadline, validation, and publication semantics. |
| 4. Courses and learning resources | Faculty course edit, roster, resource upload/download, announcement creation; student courses, resources, announcement detail; faculty schedule editor. | Course ownership/enrollment restrictions and file flows work. Changes appear in the opposite role's view after refresh. |
| 5. Dashboards and calendar | Complete both dashboards, quick actions, statistics, schedule, deadlines, announcement cards, notification popover, month navigation, event creation and display. Reuse shared visual components only when their behavior truly matches. | Both dashboards and calendar screens reproduce the current information hierarchy and actions at normal and minimum window sizes. |
| 6. Backend navigation cleanup | Split feature-mixed controllers and service methods into attendance, assessment, course, messaging, and account areas where the split removes actual navigation friction. Replace repository pass-through use with named service operations as those areas are touched. Preserve endpoint paths, response shapes, security rules, and database behavior. | The client still works against unchanged API contracts; a developer can locate each server operation by feature name. |
| 7. Simpler screens | Faculty/student conversations and notifications, student results view if still separate, roster/report presentation cleanup, profile/settings, notification preference, password change/relogin, empty/error/loading states, keyboard navigation. | Every listed route has a desktop destination and every existing user action has an outcome. |
| 8. Consolidation and delivery | Remove duplicate client code and unused dependencies. Update README, launch/package flow, and a concise route map. Retire React/Vite and browser-only scripts after parity review. | A new developer can find a requested change from the route map and run the desktop app using the documented steps. JavaFX client plus backend package without the TypeScript frontend. |

### QR implementation decision

Keep QR generation and image decoding in Java unless a prototype shows a concrete obstacle. A live scanner is a replaceable module. If another stack is used, run it as a local helper with a narrow input/output contract: start scan, stop scan, return QR text or a typed error. It must not hold user credentials, call attendance APIs, or make authorization decisions. Package and start/stop it with the desktop app; document its runtime requirements. An embedded web scanner should only be selected after camera access works on the target operating systems.

## Code quality rules for every phase

1. One named screen per user-facing feature; no path-string switches or role conditionals spread across views.
2. Use typed Java records for API payloads and responses. Keep JSON parsing and HTTP status handling in the API layer.
3. Keep JavaFX UI updates on its application thread; make network and camera work asynchronous. Centralize loading, error, and empty-state patterns.
4. Preserve server-side authorization and validation. The client only improves feedback and cannot grant permissions.
5. Use a small JavaFX CSS theme: design tokens, shell, shared controls, then feature styles. Prefer named style classes to inline styles. Reuse existing image assets and avoid copied browser CSS overrides.
6. Extract a shared component only after at least two screens use the same behavior. Keep feature-specific code near its screen.
7. Comment only non-obvious constraints and decisions: time zones, QR payload/expiry, CSRF/session behavior, file validation, and background-thread boundaries. Use descriptive class and method names for routine behavior.
8. Update the route map and phase status in the same change as each completed screen. Remove obsolete code after parity, rather than keeping parallel implementations indefinitely.

## Model-switch handoff protocol

At every phase boundary, update a short `docs/desktop-progress.md` with: completed screens and files, exact launch command, current API and QR decisions, known gaps, visual differences, and the next phase's first task. Do not mark a phase done from a compile alone; use one meaningful end-to-end workflow and inspect its screen. A later model should read this plan, the progress file, and the route map, then work only on the next incomplete phase. Keep commits scoped to one phase or feature so another model can review and continue without reconstructing intent from a large diff. **The intended model-switch point is after phase 6:** the platform, QR, file/assessment flows, complex visual screens, and backend organization will be complete; the remaining work is mostly bounded screens and cleanup.

The current `README.md`, `QA_REPORT.md`, `STUDENT_QA_REPORT.md`, `frontend/e2e/`, and `qa/screenshots/` are reference material. Their recorded runs are historical evidence; the new desktop app requires its own verification during implementation.
