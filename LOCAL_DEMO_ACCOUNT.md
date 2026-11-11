# Local Faculty demo account

- Login URL: http://127.0.0.1:5173/login/faculty
- Faculty ID: **FACULTY001**
- Display name: **Demo Faculty**
- Role: **FACULTY**
- Supplied password authentication: **succeeded** through the actual Java API and Playwright, including after restarting with seeding disabled.
- Storage verified: BCrypt password hash; `local_only=true`. No plaintext password is stored in source/configuration or the database password column.

The pre-provisioning H2 inspection found no existing FACULTY001 or other Faculty/Student records. A pre-change database backup is retained privately under `.tools/local-demo-backup/`. The seeded examples are explicitly synthetic: three courses, three assessments, four sessions (three today plus one historical), one attendance record and one submission. Existing record-preservation and conflict scenarios were tested against separate H2 databases.

The account persists in the local `data/ams.mv.db` database. Ordinary local startup does not need seeding or a password environment variable. All 20 Java tests passed on JDK 21, including eight new provisioning/profile-policy tests. Both API and browser login succeeded, the Faculty Dashboard loaded, and no browser console errors or failed responses were observed. Website design and Student Dashboard files were not changed.

Seeding is opt-in through `scripts/start-local-faculty-demo.ps1`; it prompts securely for the password rather than embedding one. An existing identifier must match the requested name, role and credentials or provisioning fails without overwriting it. The seeder and login policy require the sole `local` profile and an H2 datasource. A copied local-only user is rejected outside that environment. MySQL/staging/production policy branches were tested; no real MySQL server was used for this check.

Verification evidence: [JSON results](qa/results/local-faculty-demo.json) and [Faculty Dashboard screenshot](qa/screenshots/local-faculty-demo-dashboard.png).
