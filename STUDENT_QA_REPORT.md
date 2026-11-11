# Phase 3 - Student Portal QA Report

Completed: **11 November 2026, 02:46 IST**. Workspace: `C:\Users\tando\Downloads\thapar-academic-management`.

**Student login:** http://127.0.0.1:5173/login/student  
**Student dashboard:** http://127.0.0.1:5173/student/dashboard (requires Student authentication)  
**Main website:** http://127.0.0.1:5173/

## Final executed results

| Verification | Passed | Failed / skipped |
| --- | --- | --- |
| Student Playwright functional tests | **44** (11 per viewport) | **0 / 0** in the completed per-viewport results |
| Existing Faculty Playwright regression tests | **60** (15 per viewport) | **0 / 0** |
| Final Student LMS download/layout regression | **4** (one per viewport) | **0 / 0** |
| Additional Student login/logout checks in Faculty runner | **4** | **0 / 0** |
| Java 21 tests | **37** | **0 failures, 0 errors, 0 skipped** |
| Existing real HTTP smoke checks | **39** | **0** |
| Frontend production build | **Passed** (TypeScript + Vite) | No build errors |
| Faculty database restart persistence canary | **4/4** | No lost events |
| Existing FACULTY001 on the main database | API login, browser login and Faculty Dashboard passed | No console errors or failed requests |
| Main Vite/Java API connectivity | HTTP 200 from page, backend health and Vite proxy | No unexpected browser errors |

No retries were enabled. The first full Student matrix had one mobile download timeout; it is documented below. The complete mobile suite and the affected download flow at all four sizes were rerun successfully after the fix. The table combines the latest completed run for each viewport; the mobile suite was repeated after the fix.

Evidence: [combined summary](qa/results/student-phase3-summary.json), [Java results](qa/results/student-phase3-java.json), [browser network audit](qa/results/student-phase3-network.json), [HTTP smoke output](qa/results/student-http-smoke.txt), [build output](qa/results/student-production-build.txt), [main startup](qa/results/local-startup.json), and [existing Faculty account verification](qa/results/local-faculty-demo.json).

## Implementation

- Approved Student Dashboard implemented as HTML/CSS and reusable React components using the supplied campus imagery and actual Thapar logo. The original reference was not used as a page background.
- Maroon collapsible sidebar, mobile drawer, working search/notifications/profile/logout, welcome banner, four real-data statistics, schedule, deadlines, announcements, student profile and calendar.
- Quick Actions contains **exactly three** evenly distributed controls: **View Timetable**, **Submit Assignment**, **Check Results**. No extra class-joining control was added.
- All eight Student routes work: Dashboard, Messages, Attendance, Courses & LMS, Assessments, Calendar, Results, Settings.
- Official CGPA is a nullable stored field. Both dashboard and Results show **Not available** when absent. Marks never generate CGPA. Synthetic records are explicitly labeled; no screenshot name/roll number was substituted for the authenticated student's record.
- Student data uses the existing session authentication and shared Faculty database. Identity is derived from the principal; active enrollment, ownership, roles and CSRF are enforced server-side.
- Student submissions support text and validated PDF/TXT attachments; Faculty can download submitted work, enter marks and feedback, and publish results. Unpublished marks/feedback are not returned to students.
- Faculty class-information editing is available in its Calendar module. Existing Faculty Dashboard structure and approved styling were preserved.

## Browser matrix and screenshots

Playwright **1.65.0**, Chromium **158.0.8127.18**, Java 21.0.13.0.2, timezone Asia/Kolkata. Screenshots use the requested viewport but capture the entire page; vertical scrolling on smaller screens is expected.

| Viewport | Dimensions | Student suite | Faculty suite | Download regression | Student screenshot |
| --- | --- | --- | --- | --- | --- |
| Desktop | 1920 x 1080 | 11 passed | 15 passed | 1 passed | [Dashboard](qa/screenshots/student-desktop-dashboard.png) |
| Laptop | 1366 x 768 | 11 passed | 15 passed | 1 passed | [Dashboard](qa/screenshots/student-laptop-dashboard.png) |
| Tablet | 768 x 1024 | 11 passed | 15 passed | 1 passed | [Dashboard](qa/screenshots/student-tablet-dashboard.png) |
| Mobile | 390 x 844 | 11 passed | 15 passed | 1 passed | [Dashboard](qa/screenshots/student-mobile-dashboard.png) |

The approved reference is [student-dashboard.png](references/student-dashboard.png). Rendered screenshots were inspected for sidebar/header proportions, hero, glass statistics, three Quick Actions, two-panel schedule/deadline arrangement on desktop, announcements, profile card, calendar, typography, spacing and responsive behavior. No page-wide horizontal overflow was found in the final route matrix or populated LMS download checks.

| Student module | Desktop | Laptop | Tablet | Mobile |
| --- | --- | --- | --- | --- |
| Dashboard | [desktop](qa/screenshots/student-desktop-dashboard-module.png) | [laptop](qa/screenshots/student-laptop-dashboard-module.png) | [tablet](qa/screenshots/student-tablet-dashboard-module.png) | [mobile](qa/screenshots/student-mobile-dashboard-module.png) |
| Messages | [desktop](qa/screenshots/student-desktop-messages-module.png) | [laptop](qa/screenshots/student-laptop-messages-module.png) | [tablet](qa/screenshots/student-tablet-messages-module.png) | [mobile](qa/screenshots/student-mobile-messages-module.png) |
| Attendance | [desktop](qa/screenshots/student-desktop-attendance-module.png) | [laptop](qa/screenshots/student-laptop-attendance-module.png) | [tablet](qa/screenshots/student-tablet-attendance-module.png) | [mobile](qa/screenshots/student-mobile-attendance-module.png) |
| Courses & LMS | [desktop](qa/screenshots/student-desktop-courses-module.png) | [laptop](qa/screenshots/student-laptop-courses-module.png) | [tablet](qa/screenshots/student-tablet-courses-module.png) | [mobile](qa/screenshots/student-mobile-courses-module.png) |
| Assessments | [desktop](qa/screenshots/student-desktop-assessments-module.png) | [laptop](qa/screenshots/student-laptop-assessments-module.png) | [tablet](qa/screenshots/student-tablet-assessments-module.png) | [mobile](qa/screenshots/student-mobile-assessments-module.png) |
| Calendar | [desktop](qa/screenshots/student-desktop-calendar-module.png) | [laptop](qa/screenshots/student-laptop-calendar-module.png) | [tablet](qa/screenshots/student-tablet-calendar-module.png) | [mobile](qa/screenshots/student-mobile-calendar-module.png) |
| Results | [desktop](qa/screenshots/student-desktop-results-module.png) | [laptop](qa/screenshots/student-laptop-results-module.png) | [tablet](qa/screenshots/student-tablet-results-module.png) | [mobile](qa/screenshots/student-mobile-results-module.png) |
| Settings | [desktop](qa/screenshots/student-desktop-settings-module.png) | [laptop](qa/screenshots/student-laptop-settings-module.png) | [tablet](qa/screenshots/student-tablet-settings-module.png) | [mobile](qa/screenshots/student-mobile-settings-module.png) |

On narrower screens the schedule uses stacked rows, modules use a drawer, and dashboard panels reflow. Wide analytical module tables use contained horizontal scrolling rather than overflowing the page. Search is available through a mobile search toggle. Resource download targets are at least 44 pixels.

Visible reference differences: the existing approved portal uses a monochrome treatment of the actual logo on dark backgrounds, campus photo crops constrain framing, and the sidebar retains the existing TIET decoration because no separate architectural illustration asset was supplied. The selected-day event list makes the calendar taller than the reference. Student names, counts, deadlines, academic metadata and unavailable CGPA intentionally reflect backend records, not the reference's example numbers. Manual visual comparison was performed; no pixel-perfect similarity score is claimed.

## Faculty-Student synchronization actually exercised

| Faculty action / shared workflow | Executed Student-side result |
| --- | --- |
| Create assignment through Faculty UI | New assignment appeared in Student Assessments after navigation/refresh |
| Student submits text and TXT attachment | Submission appeared in Faculty Assessments; owning Faculty downloaded the file |
| Grade a submission | Student Results remained empty for that assessment before publication |
| Publish results | Student saw marks **87/100** and the entered Faculty feedback |
| Edit an already published mark (Java integration) | Result became hidden again until republication |
| Student sends message; Faculty replies | Both real inbox histories displayed the counterpart's message after refresh |
| Faculty uploads material | Student LMS displayed it and downloaded the attachment |
| Faculty posts announcement and description | Student read the full announcement in the detail dialog |
| Faculty generates attendance QR | Student decoded the actual QR image locally and successfully claimed attendance |
| Replay attendance token | Server returned 409 and the Student UI displayed clear duplicate feedback |
| Faculty creates academic event | Student Calendar displayed it on the selected date |
| Faculty edits classroom information | Student timetable/calendar reflected **QA-LT-303** after refresh |

All workflows ran with separate authenticated Faculty and Student browser contexts sharing the real Java server/database. The existing error/retry test deliberately injects a 503 only to verify recovery feedback.

## Security and data checks

The 37 Java tests include the original authentication/security/Faculty suite, local demo policy/provisioning, 13 Student integration tests, and four Student demo seed tests. They verify:

- Role denial, anonymous protection, course ownership and active enrollment authorization.
- Other students' submission attachments cannot be downloaded or included in own-submission responses.
- Messages to unrelated teachers are rejected; normal Faculty/Student conversation persistence works.
- Unpublished grades and feedback are hidden; CGPA remains a stored academic value.
- Overdue, empty, duplicate, unsupported-file, forged-PDF and oversized submissions are rejected.
- CSRF protection remains enabled; duplicate attendance claims are rejected.
- Current-password checks and BCrypt hashing remain intact for Student password changes.
- Student profile requests cannot change official CGPA through extra supplied fields.
- Local Student demo creation is BCrypt-based, idempotent, conflict-aware and restricted to the sole local profile/H2. Unrelated Faculty credentials are preserved.

Browser tests additionally verify UI password changes, CSRF refresh after sign-out, logout protection, notification preferences, search, announcement detail keyboard dismissal, month/date interactions, navigation collapse/drawer behavior, and upload/error feedback. Positive paths had no unexpected console errors, uncaught exceptions or failed requests. Negative paths intentionally produced 400, 401, 403, 409 or the injected 503; the retained network audit distinguishes these from failures.

## Database and preservation

Flyway **V3__student_portal.sql** adds:

- Nullable Student academic year, semester and official CGPA.
- Active enrollment flag (defaults true for existing records).
- Grade feedback.
- Announcement description and publication timestamp.
- Submission filename, content type and attachment bytes.

Existing entities and relationships are reused. No tables or Faculty/Student records were deleted/reset. The main database was copied privately to `.tools/phase3-backup/ams-before-student-portal.mv.db` before migration. Existing FACULTY001 and its three courses, three assessments, four sessions and attendance record were verified through real API/browser authentication after migration.

Mutating browser tests used isolated H2 databases under `.tools/qa/`, Vite 5174 and Java 8082. The main application uses 5173/8080. The attendance fixture changes only guarded QA databases to create an active session at test time. Production authentication, enrollment checks, token expiry and login throttling were not weakened. Test passwords are generated in process memory and not hardcoded; traces/videos/storage-state files are disabled.

## Issues found and fixed

| Issue | Resolution and proof |
| --- | --- |
| Copied Faculty footer label appeared in the Student sidebar | Changed only the Student layout role label; final screenshots show Student |
| Route changes could reuse a module's differently shaped data | Student module routes now have distinct component keys, with safe array checks for assessment/calendar rendering |
| Mobile course-material download timed out despite no HTTP error | Gave resource rows shrinkable content and the download link a stable 44-pixel inline-flex touch target; complete mobile suite and populated download/overflow checks passed at all four sizes |
| Initial QR camera callback had a syntax error during build | Corrected before browser execution; production build and real QR-image decoding passed |

Historical failure evidence: [mobile download JSON](qa/diagnostics/student-mobile-before-download-fix.json) and [failure context](qa/diagnostics/student-mobile-download-before-fix.md). The final results have no unresolved failed or skipped scenarios.

## Local Student demo credentials

No fixed/default Student password was introduced. The main application is left running with seeding disabled. To create your own reusable local-only Student account linked to the existing FACULTY001 synthetic courses, run from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local-student-demo.ps1 -StudentId STUDENT001 -DisplayName 'Demo Student' -FacultyId FACULTY001 -Restart
```

Enter your chosen password at the secure prompt (at least 12 characters). The script stops the port-8080 Java process only if it can verify it belongs to this project, then starts the local H2 seed mode. An unrecognized process is not stopped. Existing matching Student credentials are verified; conflicts are not overwritten. The account persists in H2 and works with ordinary local startup afterward. Other Faculty/Student accounts and their work are preserved. The seeded student's program/year/semester are visibly synthetic; official CGPA is left null.

If using another local Faculty demo, pass its identifier instead. It must already exist, be marked local-only and have synthetic courses. No real production Faculty is populated by this demo workflow. The local-only login policy rejects these demo users in non-local profiles or non-H2 databases.

## Remaining limitations

- Real camera hardware/permission behavior was not exercised. Local QR-image decoding, manual-token plumbing, successful claims and duplicate rejection were exercised. **No GPS or 20-meter proximity check exists.**
- Uploads are limited to PDF signatures and valid UTF-8 TXT up to 5 MB and downloaded as attachments. Antivirus/PDF active-content sanitization is not implemented. Initial submissions are final; resubmission/versioning is not implemented.
- Assignments and basic quiz submissions are available; a complete question-bank/timed-exam engine is not implemented.
- Official CGPA can be displayed when authorized data is stored. There is no registrar import/admin workflow for official SGPA/transcripts, and no invented grade-to-CGPA formula.
- Student access currently follows active enrollments. A separate historical/alumni course-access policy is not implemented.
- MySQL 8, Firefox/WebKit, physical mobile devices, production HTTPS deployment and a full accessibility audit were not runtime-tested in this phase. H2/Flyway and Java 21 were tested.
- Search/lists are suitable for the initial small dataset; large deployments need pagination and shared/distributed rate/session infrastructure.

## Reproduce tests

```powershell
powershell -ExecutionPolicy Bypass -File backend/mvnw.ps1 test
npm.cmd run test:student --prefix frontend
$env:QA_REPORT_TAG = 'faculty-phase3'
node scripts/run-browser-qa.mjs
Remove-Item Env:QA_REPORT_TAG
python scripts/smoke_test.py
npm.cmd run build --prefix frontend
```

Use JDK 21 for Java/HTTP commands. Keep the main localhost site available for the existing exact-URL browser check. QA servers are temporary and stopped by their runners. The main site remains **http://127.0.0.1:5173/**. Additional endpoint/setup details are in [README.md](README.md).
