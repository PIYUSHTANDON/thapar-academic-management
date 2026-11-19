# Desktop route and API ledger

Phase 0 baseline for the JavaFX migration. Source revision inspected: `8e2e6c1` (`feature/collapsible-sidebar-and-ui-modernization`). The paths below are current web URLs; the desktop client will use the corresponding named screens. All API paths have prefix `/api/v1`. `F` and `S` mean faculty and student.

## Source of truth and display target

- Behavior: current `frontend/src/` and `backend/src/main/`. The backend owns permissions, validation, persistence, and attendance decisions.
- Appearance: current `frontend/src/styles.css`, `student.css`, logos/campus assets, and `references/`. `qa/screenshots/` show earlier verified browser states. Some screenshots show a top header/search bar that the current layout TSX does not render; treat these as historical visual references, not a requirement to restore that header.
- Initial JavaFX review sizes: normal window **1366 × 768**, minimum **1000 × 680**, optional expanded **1920 × 1080**. These are provisional desktop targets; the browser QA also used tablet/mobile sizes, but the new product is a desktop app. Keep content scrollable at minimum size. Compare hierarchy, spacing, colors, imagery, readability, and states rather than browser pixel coordinates.
- Shared visual cues from the screenshots: deep maroon sidebar, brighter red active navigation, pale gray page background, white rounded panels, dark serif display headings, compact sans-serif body text, campus-photo dashboard hero, dark translucent statistic cards, red primary actions, and colored status badges. Existing logo and campus assets remain available.

## Screen checklist

Each row gives the future JavaFX screen name, current implementation, user actions, and data calls. `GET` calls are shown without `/api/v1`.

| Role and current URL | JavaFX destination | Current source and actions | Read data / writes |
| --- | --- | --- | --- |
| Public `/` | `RoleSelectionView` | `pages/auth/Login.tsx`: choose faculty or student | None |
| Public `/login/faculty`, `/login/student` | `LoginView` | Role-specific identifier, password visibility, remember-me, helpdesk recovery guidance, validation | Auth login/session/CSRF; logout in shared shell |
| F `/faculty/dashboard` | `FacultyDashboardView` | `pages/faculty/Dashboard.tsx`: stats, today's schedule, quick links, calendar, draft actions, notifications | GET `/faculty/dashboard`, `/schedule`, `/calendar`, `/notifications`; POST notification read |
| F `/faculty/messages` | `FacultyMessagesView` | `Modules.tsx`: choose enrolled student, view conversation, send message | GET `/faculty/courses`, `/students`, `/messages`; POST `/messages` |
| F `/faculty/attendance` | `FacultyAttendanceView` | Choose class, open token, show QR/countdown and attendance records | GET `/faculty/courses`, `/students`, `/attendance`, `/schedule`; POST `/attendance/sessions` |
| F `/faculty/reports` | `FacultyReportsView` | Read-only attendance table, reached from dashboard quick action | GET `/faculty/attendance`, `/schedule` |
| F `/faculty/courses` | `FacultyCoursesView` | Select course, edit title/description, upload/download resource, post announcement | GET `/faculty/courses`, `/students`, `/courses/{id}/resources`; PUT `/courses/{id}`; POST `/courses/{id}/resources`, `/courses/{id}/announcements`; GET `/resources/{id}` |
| F `/faculty/assessments` | `FacultyAssessmentsView` | Create assignment/quiz, review submissions and files, grade, publish | GET `/faculty/courses`, `/students`, `/assessments`, `/submissions`; POST `/assessments`, `/submissions/{id}/grade`, `/assessments/{id}/publish`; GET `/submissions/{id}/file` |
| F `/faculty/calendar` | `FacultyCalendarView` | Browse month/date, create event, edit class schedule | GET `/faculty/calendar`, `/schedule`, `/courses`, `/students`; POST `/calendar`; PUT `/schedule/{id}` |
| F `/faculty/students` | `FacultyRosterView` | Select course and filter roster by name/enrollment number | GET `/faculty/courses`, `/students` |
| F `/faculty/settings` | `FacultySettingsView` | Edit name/notification preference, change password | GET `/faculty/courses`, `/students`; PUT `/settings/profile`, `/settings/password`; GET `/auth/me` after profile update |
| S `/student/dashboard` | `StudentDashboardView` | `StudentDashboard.tsx`: stats, quick links, schedule, deadlines, announcements, profile card, calendar, notifications | GET `/student/dashboard`, `/profile`, `/schedule`, `/assessments`, `/announcements`, `/calendar`, `/notifications`; POST notification read |
| S `/student/messages` | `StudentMessagesView` | Choose teacher, view conversation, send message | GET `/student/courses`, `/profile`, `/messages/contacts`, `/messages`; POST `/messages` |
| S `/student/attendance` | `StudentAttendanceView` | Camera/image/manual QR input, claim, course summary and history | GET `/student/courses`, `/profile`, `/attendance`; POST `/attendance/claim` |
| S `/student/courses` | `StudentCoursesView` | Select course, inspect schedule/resources, download file, open announcement | GET `/student/courses`, `/profile`, `/schedule`, `/courses/{id}/resources`; GET `/resources/{id}` |
| S `/student/assessments` | `StudentAssessmentsView` | See status/deadline, submit text and/or PDF/TXT, download own file, inspect published feedback | GET `/student/courses`, `/profile`, `/assessments`; POST multipart `/submissions`; GET `/submissions/{id}/file` |
| S `/student/calendar` | `StudentCalendarView` | Browse month/date and class/deadline/event markers | GET `/student/courses`, `/profile`, `/calendar` |
| S `/student/results` | `StudentResultsView` | Published marks/feedback and official CGPA if available | GET `/student/courses`, `/profile`, `/results` |
| S `/student/settings` | `StudentSettingsView` | Profile details, name/notification preference, password change | GET `/student/courses`, `/profile`; PUT `/settings/profile`, `/settings/password`; GET `/auth/me` after profile update |

The current `Modules.tsx` components fetch common course/student or course/profile data even on screens that do not display it. The desktop screens should request only what they use. `/student/pending` redirects to the student dashboard. `/access-denied` and unknown paths are web-only routing states; JavaFX needs clear access-denied and unknown-destination views. Shared shell actions are navigation, profile menu, error dismissal, and sign-out.

### Implemented desktop lookup (phases 1–7)

For a request about a screen, start with the row below, then open its `api/` class for the request and its backend feature controller/service for the rule. Search for the API path when the request names an endpoint instead of a screen. `App.java` maps each `Screen` destination to its view; `css/app.css` holds presentation rules. The packaged entry point is `DesktopLauncher.java`, and `scripts/package-java-desktop.ps1` assembles the portable Windows folder.

| Change requested | Open this Java file |
| --- | --- |
| Role selection or login | `desktop/src/main/java/com/thapar/ams/desktop/ui/AuthView.java` |
| Sidebar, header, or screen switching | `desktop/src/main/java/com/thapar/ams/desktop/ui/AppShell.java`, then `App.java` and `navigation/Screen.java` |
| Faculty dashboard, quick actions, schedule, and action list | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyDashboardView.java` |
| Student dashboard, deadlines, announcements, and profile card | `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentDashboardView.java` |
| Shared dashboard hero, statistics, notifications, and schedule rows | `desktop/src/main/java/com/thapar/ams/desktop/ui/DashboardUi.java` |
| Calendar month grid, selected date, and event markers | `desktop/src/main/java/com/thapar/ams/desktop/ui/CalendarWidget.java` |
| Faculty event form and student calendar screen | `desktop/src/main/java/com/thapar/ams/desktop/ui/CalendarView.java` |
| Dashboard, calendar, and notification HTTP contracts | `desktop/src/main/java/com/thapar/ams/desktop/api/WorkspaceApi.java` |
| Faculty QR, countdown, records, or reports | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyAttendanceView.java` (reports uses its read-only mode) |
| Student QR input, claim, or history | `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentAttendanceView.java` |
| Attendance HTTP contracts | `desktop/src/main/java/com/thapar/ams/desktop/api/AttendanceApi.java` |
| QR payload or camera behavior | `desktop/src/main/java/com/thapar/ams/desktop/qr/` |
| Attendance appearance | `desktop/src/main/resources/css/app.css` (`module-` and `attendance-` selectors) |
| Faculty assessment creation, grading, publication, or submission downloads | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyAssessmentsView.java` |
| Student submission, own-file download, or published feedback | `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentAssessmentsView.java` |
| Student published results and official CGPA | `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentResultsView.java` |
| Assessment HTTP contracts and file validation | `desktop/src/main/java/com/thapar/ams/desktop/api/AssessmentApi.java`; multipart transport and authenticated bytes are in `api/ApiClient.java` |
| Faculty course edits, resource uploads/downloads, announcements | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyCoursesView.java` |
| Student course details, resources, announcements, and schedule | `desktop/src/main/java/com/thapar/ams/desktop/ui/student/StudentCoursesView.java` |
| Faculty roster search and course filter | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyRosterView.java` |
| Faculty class time, room, and cancellation inside Calendar | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyScheduleView.java` |
| Course, roster, resource, and schedule HTTP contracts | `desktop/src/main/java/com/thapar/ams/desktop/api/CourseApi.java` |
| Course screen layout helpers and appearance | `desktop/src/main/java/com/thapar/ams/desktop/ui/CourseUi.java` and `desktop/src/main/resources/css/app.css` (`course-`, `resource-`, `roster-`, and `schedule-` selectors) |
| Faculty and student conversations | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultyMessagesView.java`, `ui/student/StudentMessagesView.java`, and shared `ui/MessagesView.java` |
| Conversation contacts, history, and send contract | `desktop/src/main/java/com/thapar/ams/desktop/api/MessageApi.java` |
| Faculty and student profile, password, and notification settings | `desktop/src/main/java/com/thapar/ams/desktop/ui/faculty/FacultySettingsView.java`, `ui/student/StudentSettingsView.java`, and shared `ui/SettingsView.java` |
| Account settings and session refresh contract | `desktop/src/main/java/com/thapar/ams/desktop/api/AccountApi.java`; CSRF state is in `api/ApiClient.java` |
| Shared loading, error, and empty labels | `desktop/src/main/java/com/thapar/ams/desktop/ui/UiFeedback.java` |

The screen checklist above records the original browser behavior and planned destinations. Implemented JavaFX views call only the endpoints they use; for example, phase 2 attendance does not fetch courses or profiles merely to render its page.

Every `Screen` destination now has a JavaFX view. Keyboard shortcuts in the shell are **Ctrl+1** for the role dashboard, **Ctrl+2** for messages, and **Ctrl+,** for settings. Buttons and form fields also use standard Tab, Space, and Enter navigation. Notification items can be marked read from the dashboard menu or Settings. Changing a password returns to role-specific sign-in; the client clears the old CSRF token so the new login starts a fresh session.

### Backend lookup (phase 6)

Start with the role and feature. Each controller owns that feature's HTTP routes and input validation; the matching service owns permission checks, queries, and writes. Endpoint paths and response fields remain those listed below.

| Change requested | Controller | Service |
| --- | --- | --- |
| Faculty dashboard, calendar, academic event | `FacultyOverviewController` | `FacultyOverviewService` |
| Faculty courses, roster, resources, announcements, schedule edits | `FacultyCourseController` | `FacultyCourseService` |
| Faculty attendance records and QR session token | `FacultyAttendanceController` | `FacultyAttendanceService` |
| Faculty assessments, grading, publication, submission files | `FacultyAssessmentController` | `FacultyAssessmentService` |
| Faculty messages | `FacultyMessageController` | `FacultyMessageService` |
| Faculty notifications, profile, password | `FacultyAccountController` | `FacultyAccountService` |
| Student dashboard and calendar | `StudentOverviewController` | `StudentOverviewService` |
| Student courses, resources, announcements, schedule, search | `StudentCourseController` | `StudentCourseService` |
| Student attendance summary and claim | `StudentAttendanceController` | `StudentAttendanceService` |
| Student assessments, submissions, results, own files | `StudentAssessmentController` | `StudentAssessmentService`; `SubmissionService` handles multipart submission |
| Student messages and contacts | `StudentMessageController` | `StudentMessageService` |
| Student notifications, profile, password | `StudentAccountController` | `StudentAccountService` |

All backend paths above are under `backend/src/main/java/com/thapar/ams/`. `FacultyService` remains the shared repository/access bridge used by the existing seeders; `StudentService` retains student identity, enrollment, and time helpers. The role controllers and services return typed records from `contracts/src/main/java/com/thapar/ams/contract/` (`AuthContracts`, `OverviewContracts`, `CourseContracts`, `AttendanceContracts`, `AssessmentContracts`, `MessageContracts`, `AccountContracts`), shared at compile time with `desktop/`. `AttachmentResponse` only formats protected file downloads.

## API response fields for typed Java records

These field sets come from the current controller/service implementations. Nullable fields are explicitly marked; timestamp values are currently sent without a zone offset.

| Response | Fields used or available |
| --- | --- |
| Auth user | `id`, `name`, `identifier`, `role`, `notificationsEnabled` |
| Auth session | `authenticated`, optional `user`; CSRF response: `token`, `headerName` |
| F dashboard | `activeClasses`, `totalStudents`, `attendancePercentage`, `pendingReviews`, `actions[] {title,course,status,path}` |
| F course | `id`, `code`, `title`, `section`, `description`, `students` |
| F roster row | `id`, `userId`, `name`, `enrollmentNumber`, `program`, `courseId`, `course` |
| F schedule row | `id`, `courseId`, `course`, `startsAt`, `endsAt`, `room`, `status` |
| F assessment | `id`, `courseId`, `course`, `title`, `kind`, `deadline`, `published` |
| F submission | `id`, `assessment`, `student`, `content`, `marks` (string or `Ungraded`), `feedback`, `fileName` |
| F attendance row | `id`, `student`, `sessionId`, `course`, `recordedAt` |
| F QR creation | `token`, `expiresAt`, `sessionId` |
| F resource row | `id`, `title`, `kind` |
| S profile | `id`, `name`, `identifier`, `enrollmentNumber`, `program`, `academicYear`, `semester`, `officialCgpa` (nullable), `notificationsEnabled`, `synthetic` |
| S dashboard | `overallAttendance` (nullable), `currentCgpa` (nullable), `pendingAssignments`, `registeredCourses` |
| S course | `id`, `code`, `title`, `section`, `description`, `faculty`, `facultyUserId` |
| S attendance | `percentage` (nullable), `attended`, `total`, `courses[] {courseId,course,section,attended,total,missed,percentage}`, `records[] {sessionId,course,startsAt,room,status}` |
| S assessment | `id`, `title`, `courseId`, `course`, `kind`, `deadline`, `status`, `submissionId` (nullable), `submittedAt` (nullable), `fileName` (nullable), `content` (nullable), `marks` (nullable until publication), `feedback` (nullable) |
| S results | `officialCgpa` (nullable), `results[]` (published assessment shape) |
| S resource/announcement | `id`, `title`, `kind`, `description`, `publishedAt`, `courseId`, `course`, `faculty` |
| Shared message | `id`, `senderId`, `sender`, `receiverId`, `receiver`, `body`, `sentAt`; S contacts: `id`, `name` |
| Shared notification | `id`, `title`, `read` |
| Calendar | F: `id`, `title`, `date`; S also has `kind`. Schedule row has the same shape for F/S, though display formatting differs. |

## Write and file contract checklist

| Operation | Request and response | Rules and UI states to preserve |
| --- | --- | --- |
| Login | `POST /auth/login` JSON `identifier`, `password`, `role`, `rememberMe` → auth user | 401 wrong credentials/role; 429 throttling; rotating session ID. Remember-me extends inactivity timeout; it is not persistent authentication. |
| Logout | `POST /auth/logout` → 204 | Clear client user/CSRF state and return to role selection. |
| F message / S message | `POST /{role}/messages` JSON `receiverId`, `body` → `id` | Only authorized enrolled relationships; body max 4000. |
| Mark notification read | `POST /{role}/notifications/{id}/read` → no body | User must own notification. |
| F assessment | `POST /faculty/assessments` JSON `courseId`, `title`, `kind` (`Assignment`/`Quiz`), `deadline` → `id` | Deadline must be future; course must belong to faculty. |
| F grade and publish | `POST /faculty/submissions/{id}/grade` JSON `marks` 0–100, `feedback`; `POST /faculty/assessments/{id}/publish` | Student marks appear only after publication. |
| S submission | `POST /student/submissions` multipart `assessmentId`, optional `content`, optional `file` → `id`, `message` | PDF or UTF-8 TXT, up to 5 MB; no resubmission; deadline/enrollment checks. |
| F attendance token | `POST /faculty/attendance/sessions` JSON `sessionId` → token/expiry/sessionId | Opens from 15 min before class until class end; token expires after 3 min; new token invalidates old. |
| S attendance claim | `POST /student/attendance/claim` JSON `token` → `message` | QR payload is `{type:"thapar-attendance",token,sessionId}`; raw token also accepted. Invalid/expired/unenrolled/duplicate states differ. Server decides eligibility. |
| F course edit | `PUT /faculty/courses/{id}` JSON `title`, `description` | Course ownership; title max 255, description max 2000. |
| F resource upload | `POST /faculty/courses/{id}/resources` multipart `file` | 10 MB server request limit; authenticated course ownership. |
| F announcement | `POST /faculty/courses/{id}/announcements` JSON `title`, `description` | Creates student notifications. |
| F calendar event | `POST /faculty/calendar` JSON `title`, `eventDate` | Date format `yyyy-MM-dd`. |
| F schedule edit | `PUT /faculty/schedule/{id}` JSON `startsAt`, `endsAt`, `room`, `cancelled` | End after start; changing session clears its attendance token. |
| Profile | `PUT /{role}/settings/profile` JSON `name`, `notificationsEnabled` | Refresh `/auth/me` after success. |
| Password | `PUT /{role}/settings/password` JSON `currentPassword`, `newPassword` | New password at least 12 chars; success invalidates session and requires login. |
| File download | `GET /{role}/resources/{id}`, `GET /{role}/submissions/{id}/file` → bytes | Use same authenticated cookie jar. Preserve attachment handling, authorized ownership/enrollment, and user-visible errors. |

Additional server endpoints: `GET /api/v1/health` reports readiness; `GET /student/submissions` returns submitted assessment shapes; `GET /student/search?q=` returns up to 12 `{label,kind,path}` items. These last two are not currently needed by the rendered student screens. Faculty has no `/faculty/search` endpoint; the current roster search filters the fetched list locally.

## Cross-cutting states and visual references

- Each screen needs loading, empty, error/retry, and successful-write feedback where the current UI has it. Network work must not block JavaFX's application thread.
- Keep role-specific navigation, selected destination, sidebar expansion, avatar/profile menu, notifications and sign-out. Preserve calendar month/date selection and badges for status.
- Authenticated errors: 401 returns to sign-in; 403 shows denied action; 409 is used for duplicate attendance/submission conflicts; 429 is login throttle. Validation errors use `message` and sometimes `errors[]` from `ApiExceptionHandler`.
- Use `references/faculty-dashboard.png`, `references/faculty-login.png`, `references/student-dashboard.png`, plus current assets for main visual direction. Historical module examples: `qa/screenshots/desktop-attendance-module.png`, `desktop-assessments-module.png`, `desktop-courses-module.png`, `student-desktop-attendance-module.png`, `student-desktop-assessments-module.png`, and `student-desktop-dashboard-module.png`.
- Unsupported work remains outside this migration: GPS/proximity proof, full quiz engine, SSO, self-service password recovery, registrar CGPA import, and MySQL runtime verification. See `README.md` and `STUDENT_QA_REPORT.md` for current limits.
