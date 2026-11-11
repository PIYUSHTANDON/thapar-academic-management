# Phase 2 Browser and UI/UX QA Report

Completed: **11 November 2026, 01:43 IST**. Project: `C:\Users\tando\Downloads\thapar-academic-management`.

**Local website: http://127.0.0.1:5173/**  
Faculty login: http://127.0.0.1:5173/login/faculty  
Backend health: http://127.0.0.1:8080/api/v1/health

## Final results

| Check | Executed result |
| --- | --- |
| Full Playwright functional matrix | **60 passed, 0 failed, 0 skipped, 0 flaky**; 15 tests at each of four sizes |
| Targeted settled-screen capture matrix | **8 passed, 0 failed**; dashboard data/cookies and all eight routes rechecked at each size |
| Final mobile selector/navigation/QR regression | **3 passed, 0 failed**, plus a successful restart check |
| Supplemental actual student UI login/logout | **4 passed, 0 failed**; one flow at each size |
| Database persistence after Java termination/restart | **4/4 passed** in the full matrix; repeated successfully in the capture matrix |
| JUnit on JDK 21 | **12 passed, 0 failures, 0 errors**: 10 integration + 2 rate-limit unit tests |
| Real HTTP regression checks on JDK 21 | **39 passed** using `python scripts/smoke_test.py` |
| Frontend production build | **Passed**: TypeScript and Vite build |
| Exact localhost browser check | **Passed**: page, Java health and Vite API proxy returned 200; no console errors or failed responses |
| MySQL 8 runtime/migration | **Unverified**, unavailable locally |

The counts above describe completed final checks. Earlier failures are documented below and are not counted as passes. JSON evidence is in [qa/results](qa/results/); the main [run summary](qa/results/run-summary.json), [capture summary](qa/results/run-summary-capture.json), [student UI checks](qa/results/student-ui.json), [Java summary](qa/results/java-summary.json), [network audit](qa/results/browser-audit.json), and [exact localhost check](qa/results/local-startup.json) are retained.

## Environment and startup

- Windows; Playwright **1.65.0**, Chromium **158.0.8127.18**, headless Chromium with `Asia/Kolkata` timezone and `en-IN` locale.
- Microsoft OpenJDK **21.0.13.0.2+1-LTS**, downloaded as a portable ZIP from the [official Microsoft OpenJDK download page](https://learn.microsoft.com/en-us/java/openjdk/download). The downloaded archive matched the publisher's SHA-256 file. The system JDK was not replaced.
- A clean Java 21 compilation, packaging, JUnit run, actual server startup, browser API traffic and the HTTP smoke suite all ran successfully. Main Spring Boot startup is recorded in `.tools/local-server.log`; JDK 21 test output is in `.tools/jdk21-tests.log`.
- Main Vite **5173** proxies `/api` to Spring Boot **8080**. Both health URLs returned 200 and `ready`. [Main-site role selection screenshot](qa/screenshots/main-localhost-role.png) and [main-site faculty login screenshot](qa/screenshots/main-localhost-login.png) were captured from the exact requested URL.
- Mutating browser tests use the same frontend source at **http://127.0.0.1:5174/** with a dedicated backend on **8082**, leaving the main `data/ams` database untouched. Random faculty/student credentials exist only in the test process environment. A separate file-backed H2 database is used per viewport.
- The guarded test-only JDBC fixture opens a current class session in the isolated QA database. QR testing therefore works regardless of the time of day; it does not weaken attendance checks in application code.

## Viewport results and screenshots

Screenshots are full-page captures taken with the stated viewport. Vertical scrolling on smaller screens is expected.

| Viewport | Dimensions | Functional suite | Capture verification | Student UI login/logout | Restart persistence |
| --- | --- | --- | --- | --- | --- |
| Desktop | 1920 x 1080 | 15 passed / 0 failed | 2 passed / 0 failed | Passed | Passed |
| Laptop | 1366 x 768 | 15 passed / 0 failed | 2 passed / 0 failed | Passed | Passed |
| Tablet | 768 x 1024 | 15 passed / 0 failed | 2 passed / 0 failed | Passed | Passed |
| Mobile | 390 x 844 | 15 passed / 0 failed | 2 passed / 0 failed | Passed | Passed |

| Viewport | Faculty login | Faculty dashboard |
| --- | --- | --- |
| Desktop | [Login](qa/screenshots/desktop-login.png) | [Dashboard](qa/screenshots/desktop-dashboard.png) |
| Laptop | [Login](qa/screenshots/laptop-login.png) | [Dashboard](qa/screenshots/laptop-dashboard.png) |
| Tablet | [Login](qa/screenshots/tablet-login.png) | [Dashboard](qa/screenshots/tablet-dashboard.png) |
| Mobile | [Login](qa/screenshots/mobile-login.png) | [Dashboard](qa/screenshots/mobile-dashboard.png) |

All eight routes were opened through the actual sidebar/drawer at every viewport, with page-wide horizontal overflow assertions. The final capture pass waits for the destination API response, completed loading and a closed navigation drawer before taking screenshots.

| Module | Desktop | Laptop | Tablet | Mobile |
| --- | --- | --- | --- | --- |
| Dashboard | [desktop](qa/screenshots/desktop-dashboard-module.png) | [laptop](qa/screenshots/laptop-dashboard-module.png) | [tablet](qa/screenshots/tablet-dashboard-module.png) | [mobile](qa/screenshots/mobile-dashboard-module.png) |
| Messages | [desktop](qa/screenshots/desktop-messages-module.png) | [laptop](qa/screenshots/laptop-messages-module.png) | [tablet](qa/screenshots/tablet-messages-module.png) | [mobile](qa/screenshots/mobile-messages-module.png) |
| Attendance | [desktop](qa/screenshots/desktop-attendance-module.png) | [laptop](qa/screenshots/laptop-attendance-module.png) | [tablet](qa/screenshots/tablet-attendance-module.png) | [mobile](qa/screenshots/mobile-attendance-module.png) |
| Courses & LMS | [desktop](qa/screenshots/desktop-courses-module.png) | [laptop](qa/screenshots/laptop-courses-module.png) | [tablet](qa/screenshots/tablet-courses-module.png) | [mobile](qa/screenshots/mobile-courses-module.png) |
| Assessments | [desktop](qa/screenshots/desktop-assessments-module.png) | [laptop](qa/screenshots/laptop-assessments-module.png) | [tablet](qa/screenshots/tablet-assessments-module.png) | [mobile](qa/screenshots/mobile-assessments-module.png) |
| Calendar | [desktop](qa/screenshots/desktop-calendar-module.png) | [laptop](qa/screenshots/laptop-calendar-module.png) | [tablet](qa/screenshots/tablet-calendar-module.png) | [mobile](qa/screenshots/mobile-calendar-module.png) |
| Student Roster | [desktop](qa/screenshots/desktop-students-module.png) | [laptop](qa/screenshots/laptop-students-module.png) | [tablet](qa/screenshots/tablet-students-module.png) | [mobile](qa/screenshots/mobile-students-module.png) |
| Settings | [desktop](qa/screenshots/desktop-settings-module.png) | [laptop](qa/screenshots/laptop-settings-module.png) | [tablet](qa/screenshots/tablet-settings-module.png) | [mobile](qa/screenshots/mobile-settings-module.png) |

## Functional verification

The 15 automated scenarios run in each viewport cover:

1. Exact localhost availability, role selection, faculty branding and student login fields.
2. Required fields, show/hide password, recovery dialog and incorrect credentials.
3. Faculty login, real API statistics, all three teaching sessions, HttpOnly/SameSite session cookie and reload session restoration.
4. All eight sidebar modules, collapse/expand on desktop, and open/close/Escape behavior on tablet/mobile.
5. Calendar next/previous month, selected date, event creation and event persistence after reload.
6. Authorized course search, notification read, and all four Quick Access destinations.
7. Message sending and database-backed conversation history after reload.
8. Course description update, learning-resource upload and download, announcement persistence and student roster search/filtering.
9. Assessment creation, result publication and saved grading after reload.
10. Locally generated attendance QR, authenticated enrolled-student claim, HTTP 409 on replay and persisted attendance record.
11. Profile and notification preference persistence.
12. Anonymous route protection, student redirect to the holding page, server-side faculty API denial and CSRF denial.
13. Faculty logout, session invalidation and protected route redirect.
14. Visible loading, a deliberately injected HTTP 503, clear error display and successful real-API retry.
15. Invalid current-password feedback, password change, sign-out and successful authentication with the changed password.

The supplemental browser script submits the actual student form and signs out at every size. It asserts the authenticated role is STUDENT. The student holding page remains unchanged; no student dashboard was implemented.

Browser contexts monitor console messages, uncaught page errors, failed network requests and HTTP failures. Normal flows and the exact main-site check had **no unexpected errors**. Negative tests intentionally produced 401 (invalid login), 400 (incorrect current password), and an injected 503 (retry test). Explicit request-context authorization checks expected 401/403/409. These are verified negative scenarios, not hidden failures. The final audit confirms no other failed browser response paths or failed network requests.

## Visual comparison against supplied references

Both original PNG references and the supplied WebP logo were inspected, then compared with the rendered screenshots. This was a manual visual review, not a claimed pixel-perfect comparison score.

| Area | Observed result and adjustment |
| --- | --- |
| Thapar branding | Supplied logo retained; dark-background treatment removes the conspicuous white tile using CSS. Monochrome presentation on maroon differs from the reference's red symbol. |
| Sidebar | Maroon gradient, red active state, eight icon links and faculty profile retained. Text enlarged; sidebar can scroll on short displays. Tablet/mobile use a drawer. |
| Campus imagery | Real campus photo-only crops extracted from the supplied screenshots. Login now uses its own wider campus crop, improving the original severe zoom. No entire screenshot is used as a background. |
| Welcome and statistics | White serif welcome heading, dark image overlay and four glass cards preserved. Statistics were asserted against the actual Java API, not the reference's example values. |
| Quick Access | Four maroon-icon controls retained, with responsive wrapping and verified destinations. |
| Schedule | Table retained on larger screens. At mobile width it becomes stacked rows showing course, time, room and status together. |
| Calendar | Backend event indicators, actual current month/date, month navigation and selected-day state verified. |
| Action Required | Draft assessment actions, course labels and navigation retained. Content follows QA database records rather than reference text. |
| Typography/spacing | Small navigation, statistics, calendar and action labels enlarged. Header and login spacing adjusted while retaining maroon, neutral panels, borders, rounded corners and subtle shadows. |

Remaining reference differences: no clean architectural line-art asset exists, so the sidebar still uses the existing TIET/EST. 1956 decoration; source-photo crops constrain framing; the supplied square logo differs from the screenshot's horizontal lockup. Faculty name, statistics, calendar events and session status intentionally reflect synthetic backend data and current time. No unrelated campus assets or invented real student records were introduced.

## Defects found and fixes verified

| Finding | Fix and verification |
| --- | --- |
| Repeated 401 console noise on normal signed-out startup | Added safe public `/auth/session` status probe; `/auth/me` and role APIs remain protected. Added a Java integration check. Main browser audit is clean. |
| Missing favicon 404 | Added the supplied logo as favicon. Final browser audits have no asset 404s. |
| Cramped tablet header and unavailable mobile search | Drawer breakpoint moved to 1000px and mobile search toggle added. Search and all eight routes passed at tablet/mobile sizes. |
| Mobile teaching table hid room/status in a horizontal scroller | Added stacked mobile schedule layout; final screenshot shows all fields with no page-wide horizontal overflow. |
| Mobile attendance session selector was unnecessarily narrow | Stacked the inline form on mobile so the selector spans the panel width. Dashboard, eight routes and QR attendance passed in a focused three-test rerun. |
| Small labels and mismatched white logo tile/campus framing | Adjusted typography, CSS dark-background logo treatment and login campus crop. Final screenshots inspected. |
| Success toast appeared while module refresh was still loading | Success now follows a successful refresh, avoiding contradictory live status messages. Profile and other save flows passed. |
| Password change fetched faculty data after sign-out and reused an expired CSRF token | Suppressed post-change faculty refresh and refreshed CSRF before returning to sign-in. Changed-password reauthentication passed at all sizes. |
| Immediate Java termination could lose the latest H2 event with default delayed writes | Enabled `WRITE_DELAY=0` for local and QA file databases. Save/restart/relogin/read checks passed at all four sizes, twice. |
| Mobile room prefix contained an encoding replacement character | Replaced with plain `Room:`; final mobile screenshot inspected. |
| Screenshots could capture a closing drawer or a pre-effect loading race | Screenshot helper now waits for the destination API response and completed loading, closes the drawer and disables transitions for capture. Eight targeted capture tests passed. |

Early desktop runs had two locator timeouts, a strict status-locator conflict, an assertion encoding mismatch and cascading rate-limit failures after worker restarts. Locators were corrected to accessible roles, status sequencing and encoding were fixed, and the production throttle was retained. A later run exposed the password-flow bug, then the independent restart check exposed the H2 write-delay issue. Historical diagnostics are retained in [qa/diagnostics](qa/diagnostics/). None of these unresolved failures remain in the final matrices.

## Compatibility and limitations

- **JDK 21: verified.** Backend compiled, packaged, started and tested on JDK 21, including 39 real HTTP checks and all browser traffic.
- **MySQL 8: unverified.** No `mysql`/`mysqld` executable or MySQL service was found in the checked locations; port 3306 was closed. Docker CLI exists, but Docker Engine was not running, so no MySQL container was available. No MySQL compatibility pass is claimed.
- Chromium was tested; Firefox, WebKit, physical iOS/Android devices, production HTTPS deployment and a full accessibility/screen-reader audit were not tested. Responsive checks use emulated viewports and mobile/touch flags.
- Persistence was verified for each module's available writes after browser reload, plus a calendar canary after actual Java termination/restart. This is not exhaustive crash recovery or load testing of every table.
- Forgot-password helpdesk guidance is available; self-service recovery and SSO remain unconfigured. Quiz authoring, proximity/GPS checks and a student scanning/submission interface remain future work.
- The student dashboard is intentionally pending its separate design.

## Reproduce

Keep the main site running at 5173/8080, build the backend, and run:

```powershell
$env:PLAYWRIGHT_BROWSERS_PATH = "$PWD/.tools/playwright"
frontend/node_modules/.bin/playwright.cmd install chromium
npm.cmd run test:e2e --prefix frontend
npm.cmd run build --prefix frontend
```

`npm run test:e2e` generates credentials in memory, runs each viewport against isolated servers, performs restart checks, runs the supplemental student forms, and stops its temporary servers. Reports use no retries. Traces, videos and storage-state files are disabled to avoid retaining authentication secrets. The Java integration tests also use runtime-generated passwords.

For targeted screenshot rechecks, set `QA_REPORT_TAG=capture` in the process environment and run `node scripts/run-browser-qa.mjs --grep 'all eight sidebar modules|faculty UI login'`. Java and HTTP commands are in [README.md](README.md). The final main application remains available at **http://127.0.0.1:5173/**; temporary QA servers are stopped after checks.
