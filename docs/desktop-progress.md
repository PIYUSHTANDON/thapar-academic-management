# JavaFX migration progress

Updated: 2026-11-19 (Asia/Kolkata)

PostgreSQL follow-up: Docker PostgreSQL is configured and running at `127.0.0.1:55432/thapar_ams`, schema `academic`. All three migrations applied, Hibernate validated the schema, and the backend API started on port 8082. The existing backend suite now passes all 37 tests and Maven packaging succeeds. See `docs/postgres.md`; earlier missing-plugin notes below are historical. The database is fresh; H2 records have not been imported.

Shared contracts & automated tests follow-up: Introduced a parent Maven reactor (`pom.xml`, `mvnw.ps1`) with a lean `contracts/` module (`7` feature-grouped record files under `com.thapar.ams.contract.*`). Migrated all 13 backend REST controllers, `ApiExceptionHandler`, and 12 feature services off untyped `Map`s/inline records onto `contracts/`, and removed all 37 duplicated wire records + `model/User.java` from `desktop/`. Hardened `ApiClient` with `JavaTimeModule` ISO-8601 date/time handling and structured `ApiErrorResponse.errors()` parsing, and fixed `App.java` to close `AutoCloseable` views (`Timeline` / `WebcamQrScanner`) on `logout()`, `401` redirects, and password changes. Verified full reactor `mvnw.ps1 test` (`41` backend tests including `ContractSerializationTest` + `9` desktop unit tests including `ApiClientTest` and `DesktopApiTypedSerializationTest`) and live E2E `DesktopBackendContractIT` (`1` full cross-role workflow test).

## Current checkpoint

**Phases 0–6 are implemented and verified at their stated checkpoints. Phase 7 is implemented and code/API tested. Shared API contracts (`contracts/`) and automated desktop/backend contract tests are implemented and passing. Phase 8's Java-first launch, packaging script, and documentation are implemented; visual parity and final React/Vite retirement remain deferred at the user's request.** The screen/action/API inventory, response shapes, write contracts, backend feature lookup, visual references, target desktop sizes, and current-versus-historical screenshot distinction are in `docs/desktop-route-map.md`. The ordered phases and quality rules are in `JAVA_DESKTOP_MIGRATION_PLAN.md`. Earlier screens still need a minimum-size review, and Phase 2 live camera decode remains open.

## Files changed

- `JAVA_DESKTOP_MIGRATION_PLAN.md`: migration sequence and model-switch checkpoint.
- `docs/desktop-route-map.md`: phase 0 baseline and implementation checklist.
- `docs/desktop-progress.md`: resume checkpoint.
- `desktop/pom.xml`: JavaFX 21, Jackson, ZXing, and webcam-capture desktop module, targeting Java 21 bytecode.
- `desktop/mvnw.ps1`: repository-local Maven launch wrapper for the desktop module.
- `desktop/src/main/java/com/thapar/ams/desktop/`: application startup, API client, user model, named navigation, role/login views, shell, and dashboard visual slice.
- `desktop/src/main/java/com/thapar/ams/desktop/api/AttendanceApi.java`: typed faculty and student attendance requests and responses.
- `desktop/src/main/java/com/thapar/ams/desktop/qr/`: QR payload codec, scanner interface, and Java webcam adapter.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyAttendanceView.java`: session selection, QR/expiry display, and faculty records/reports.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentAttendanceView.java`: manual, image, and camera input; claim; summary and history.
- `desktop/src/test/java/com/thapar/ams/desktop/qr/QrCodecTest.java`: QR round-trip and invalid payload checks.
- `desktop/src/main/java/com/thapar/ams/desktop/api/AssessmentApi.java`: typed assessment, submission, grade, result, and file endpoints.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyAssessmentsView.java`: assessment creation, submission review/download, grading, and publication.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentAssessmentsView.java`: assessment list, text/file submission, own-file download, and published feedback.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentResultsView.java`: published marks and official CGPA.
- `desktop/src/main/java/com/thapar/ams/desktop/api/CourseApi.java`: typed course, roster, resource, announcement, and schedule endpoints.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyCoursesView.java`: course editing, resource upload/download, and announcements.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentCoursesView.java`: enrolled course details, resources, announcement detail, and class schedule.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyRosterView.java`: searchable, course-filtered enrolled student roster.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyScheduleView.java`: class time, room, and cancellation editor in the Calendar destination.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/CourseUi.java`: small shared course-screen layout helpers.
- `desktop/src/main/java/com/thapar/ams/desktop/api/WorkspaceApi.java`: typed dashboard, calendar, notification, and event endpoints.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/DashboardUi.java`: shared hero, statistics, notification menu, action buttons, and schedule rows.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyDashboardView.java`: faculty quick actions, teaching schedule, calendar, and pending actions.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentDashboardView.java`: student quick actions, schedule, deadlines, announcements, profile, and calendar.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/CalendarWidget.java`: selectable month grid, event markers, and selected-day list.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/CalendarView.java`: faculty event creation and student calendar; the faculty schedule editor is embedded here.
- `desktop/src/main/resources/`: JavaFX CSS and three existing visual assets.
- `README.md`: desktop launch summary and links to the migration documents.
- `.gitignore`: excludes `desktop/target/` build output.
- `backend/src/main/java/com/thapar/ams/controller/`: faculty and student routes split into feature controllers; `AttachmentResponse` holds the shared download response formatting.
- `backend/src/main/java/com/thapar/ams/service/`: faculty and student feature services now own their named operations; `FacultyService` and `StudentService` retain shared access helpers.
- `desktop/src/main/java/com/thapar/ams/desktop/api/MessageApi.java`, `AccountApi.java`: typed conversation and account requests.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/MessagesView.java`, `SettingsView.java`: shared conversation and settings behavior behind role-named destinations.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/UiFeedback.java`: small shared loading, error, and empty-state labels.
- `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyMessagesView.java`, `FacultySettingsView.java`, `ui/student/StudentMessagesView.java`, `StudentSettingsView.java`: role-named final destinations.
- `desktop/src/main/java/com/thapar/ams/desktop/DesktopLauncher.java`, `desktop/pom.xml`: a plain Java entry point and JAR manifest for portable launch.
- `scripts/package-java-desktop.ps1`, `run-packaged-desktop.ps1`, `run-packaged-server.ps1`: assemble and run a portable Windows distribution with JavaFX and the backend.
- `README.md`, `.gitignore`: Java-first local and package instructions; ignore generated distribution output.

## Next task

When visual review resumes, inspect Phase 7 Messages and Settings at 1366 × 768 and 1000 × 680, recheck the conversation composer after its height adjustment, verify live camera decode with a QR if available, and complete parity review before retiring React/Vite. Retry backend and desktop Maven packaging when the missing plugins are cached or Maven Central is reachable. Keep the browser reference in place until that comparison is complete.

## Run the current desktop slice

1. Start the existing Spring Boot backend on port 8080 using the instructions in `README.md` (or point the client to another local server).
2. From the repository root, run `powershell -NoProfile -ExecutionPolicy Bypass -File desktop/mvnw.ps1 javafx:run` with JDK 21+.
3. If the API is on another port, set `AMS_API_BASE` to a full prefix such as `http://127.0.0.1:8081/api/v1` before launching JavaFX.

The desktop module is deliberately a separate client during development. It has no Node/Vite dependency. All named destinations have JavaFX views; phase 8 handles delivery and removal of the browser implementation after parity review.

For a portable Windows distribution, run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-java-desktop.ps1` from the repository root. It packages both Java modules into `dist/java-desktop/` and copies all runtime JARs, then provides `run-server.ps1` and `run-desktop.ps1`. This goal needs Maven package and dependency plugins; the current offline cache lacks them, so this script has passed PowerShell syntax review but has not completed a distribution build here.

## Decisions and open implementation work

- JavaFX is the desktop UI. JavaFX CSS and existing image assets are allowed.
- The existing backend API remains authoritative. Use typed Java client records; do not place business rules in views.
- QR image generation/decoding uses ZXing in Java. The live camera adapter uses webcam-capture behind `QrScanner`, so it can be replaced without changing the attendance views or claim API.
- Image upload currently accepts PNG/JPEG up to 5 MB; WebP is a browser-client format difference. A WebP reader can be added if parity requires it, but avoiding another imaging dependency keeps the desktop module smaller.
- Current source revision is `8e2e6c1`. Historical screenshots contain an older top header/search bar absent from current TSX layouts. Phase 5 views were checked at 1366 × 768 and 1000 × 680; earlier screens still need the minimum-size check.
- Normal/minimum window targets are provisional: 1366 × 768 and 1000 × 680.
- The API client uses an in-memory cookie jar. Restarting the desktop process requires a new login, consistent with the server's session-based remember-me behavior. The startup session check is present for a valid cookie if one is later supplied by a persistent session facility.
- Both dashboards include their role-specific cards and notifications. Faculty and student Calendar destinations use the same month grid; the faculty editor and event form sit beside it. At the minimum width, quick actions wrap, student schedule/deadline cards stack, and the month heading abbreviates to fit.

## Verification

The desktop module compiled successfully with Java 21 bytecode using JDK 26.0.2 and Maven. It launched on Windows. An isolated Spring Boot server on port 8081 used an in-memory H2 database and synthetic faculty/student accounts; neither the normal local database nor the browser frontend was changed. Both roles signed in and displayed live dashboard data. Faculty navigation, sidebar collapse, and sign-out were exercised. Role selection, login, and the representative dashboard were visually inspected at 1366 × 768. The login footer clipping was fixed; the dashboard's horizontal scrollbar was addressed in source and rechecked visually. JDK 21 runtime remains untested.

Phase 2 passed `desktop` Maven tests, including QR encode/decode round trip and invalid payload handling. A real isolated backend workflow issued a faculty attendance token, claimed it with an enrolled student, confirmed one faculty record, and rejected a duplicate claim with HTTP 409. In a later unlocked Windows session, both attendance screens were visually inspected at 1366 × 768. Faculty generated a visible QR with a live countdown. Student uploaded a fresh QR PNG, saw its token decoded, claimed successfully, and saw an attended history entry. A second manual claim displayed "Attendance already recorded." The faculty report showed that student record. A physical webcam opened and displayed live preview; a QR was not presented to the camera, so live decode remains unverified. The retest also led to readable faculty record times, an ongoing-session default selection, an untruncated QR button, and an explanation that percentages count completed classes. Wrong-role and unenrolled desktop outcomes were not manually exercised. The final code passed `desktop` Maven tests after these changes.

Phase 3 compiled and passed the `desktop` Maven tests. A Java smoke run against a separate in-memory H2 backend used the new `AssessmentApi` and `ApiClient` to create an assessment, submit text plus a UTF-8 TXT file as the enrolled student, reject a duplicate with HTTP 409, download identical file bytes through both authorized roles, grade, verify draft marks stayed hidden, publish and view results, then regrade and confirm marks stayed hidden until republished. The disposable fixture was removed after the run. In a later unlocked Windows session, the faculty Assessments, student Assessments, and student Results screens were visually inspected at 1366 × 768 against another isolated in-memory H2 backend. Faculty created `UI lifecycle assignment`; the enrolled student submitted a text response and saw it marked Submitted. Before publication, the Results screen showed no published results. Faculty saved 87/100 with feedback and published the assessment; the student Results screen then showed the assignment, mark, and exact feedback. The minimum-size visual check and JavaFX file chooser interaction remain unverified. The normal local database was not touched.

Phase 4 compiled and passed the `desktop` Maven tests. A disposable Java smoke run against an in-memory H2 backend verified the faculty roster, student enrollment, a faculty course edit visible to the student, exact resource bytes through both authenticated roles, an announcement visible to the student, a class schedule edit visible to the student, and HTTP 403 for wrong-role resource endpoints. On unlocked Windows, faculty Courses & LMS, Student Roster, the faculty schedule editor, and student Courses & LMS were visually inspected at 1366 × 768. The student course screen displayed the updated title, description, resource, announcement, and class schedule; opening announcement detail showed its exact text. A synthetic TXT resource was selected with the native JavaFX file chooser, uploaded, and appeared in the faculty resource list. Course card stretching and a faint cancellation label found during visual review were fixed and rechecked. The JavaFX download chooser, course edit form save, schedule editor save, and 1000 × 680 layout have not been manually exercised; their API paths passed the Java smoke run. The normal local database was not touched.

Phase 5 compiled and passed the `desktop` Maven tests. A disposable Java smoke run against an in-memory H2 backend deserialized both dashboards, including faculty stats/schedule and student profile/schedule/assessments; verified role-specific class calendar events; created a faculty academic event visible to the enrolled student; and marked a student notification read. After Windows was unlocked, both dashboards and calendars were visually inspected at 1366 × 768 and 1000 × 680 against isolated in-memory H2 data. Faculty month navigation worked; an event created from the JavaFX form appeared in the faculty calendar and in the enrolled student's calendar. The faculty notification count cleared after marking the synthetic notice read. Visual review found and fixed clipped action labels, month and legend text, the minimum-size sidebar account button, faculty welcome text, missing-CGPA display, and the student deadline layout. The final student dashboard showed schedule and deadline panels side by side at normal width and stacked without overlap or excess height at minimum width. The normal local database was not touched.

Phase 6 split the two mixed role controllers into 12 feature controllers and their service operations into matching feature services. HTTP inputs stay at the controller boundary; permission checks, reads, and writes now have named operations in the corresponding service. The original 26 faculty and 23 student routes were compared against the new route list: no missing, added, or duplicate routes within either role. The backend source compiled as Java 21 bytecode with parameter metadata using the dependency JARs embedded in the previous backend build. A disposable in-memory H2 run passed reads from both roles and writes for faculty calendar events, student messages, course editing, announcements, class scheduling, student attendance claims, assessment creation/grading/publication, notification reads, and student profile settings; cross-role reads showed the changes. The desktop Maven tests passed (2 QR tests). The JavaFX UI was not relaunched for this backend-only phase. The normal backend Maven test goal did not start because `spring-boot-maven-plugin:3.5.7` is missing from the local Maven cache and Maven Central DNS was unavailable. The isolated backend was stopped and the normal local database was not touched.

Phase 7 filled the two remaining role destinations with conversation and settings views. A disposable typed Java client run against an isolated in-memory H2 backend loaded faculty/student contacts and history, sent a message in each direction, loaded role-specific profiles, saved a student name and notification preference, marked a faculty notification read, changed the student's password, and signed back in with the new password on the same client. The desktop Maven tests passed (2 QR tests). A single faculty Messages preview at 1366 × 768 showed the composer below the initial viewport; the history panel was shortened in source. The user requested that visual checks be skipped for now, so the adjustment, student Messages, both Settings screens, and minimum-size layouts still need visual review. The isolated server and preview were stopped, temporary fixtures were removed, and the normal local database was not touched.

Phase 8 added a plain Java desktop launcher, a portable Windows package script, and Java-first README/route-map instructions. The package script checks that its output is inside the repository before replacing an earlier distribution. Its PowerShell files parsed cleanly, `desktop/pom.xml` parsed as XML, `git diff --check` found no whitespace errors, and the desktop Maven test goal compiled 39 source files and passed both QR tests. Java resolved the three Windows JavaFX runtime modules with `--add-modules javafx.controls`. Runtime dependencies remain in the Maven-managed package rather than copied into source. An offline desktop `package` attempt confirmed `maven-jar-plugin:3.4.2` is absent from the local cache; the script's assembled distribution therefore remains unverified. React/Vite retirement and remaining desktop visual checks are deferred at the user's request.
