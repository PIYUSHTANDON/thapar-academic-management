# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: academic.spec.ts >> assessment creation, grading and publication persist
- Location: e2e\academic.spec.ts:407:1

# Error details

```
Test timeout of 60000ms exceeded.
```

```
Error: locator.selectOption: Test timeout of 60000ms exceeded.
Call log:
  - waiting for getByLabel('Type', { exact: true })

```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary [ref=e4]:
    - link [ref=e5] [cursor=pointer]:
      - /url: /faculty/dashboard
      - img "Thapar Institute of Engineering & Technology" [ref=e6]
    - navigation "Faculty navigation" [ref=e8]:
      - link "Dashboard" [ref=e9] [cursor=pointer]:
        - /url: /faculty/dashboard
      - link "Messages" [ref=e14] [cursor=pointer]:
        - /url: /faculty/messages
      - link "Attendance" [ref=e19] [cursor=pointer]:
        - /url: /faculty/attendance
      - link "Courses & LMS" [ref=e25] [cursor=pointer]:
        - /url: /faculty/courses
      - link "Assessments" [ref=e29] [cursor=pointer]:
        - /url: /faculty/assessments
      - link "Calendar" [ref=e34] [cursor=pointer]:
        - /url: /faculty/calendar
      - link "Student Roster" [ref=e38] [cursor=pointer]:
        - /url: /faculty/students
      - link "Settings" [ref=e45] [cursor=pointer]:
        - /url: /faculty/settings
    - generic [ref=e50]:
      - generic [aria-hidden] [ref=e51]:
        - text: TIET
        - generic [ref=e52]: EST. 1956
      - link "AM Dr. Aarya Mehta (Demo) Faculty · CSE" [ref=e53] [cursor=pointer]:
        - /url: /faculty/settings
        - generic [ref=e54]: AM
        - generic [ref=e55]:
          - strong [ref=e56]: Dr. Aarya Mehta (Demo)
          - generic [ref=e57]: Faculty · CSE
  - generic [ref=e60]:
    - banner [ref=e61]:
      - button "Toggle sidebar" [ref=e62] [cursor=pointer]
      - generic [ref=e64]:
        - strong [ref=e65]: Academic Management System
        - generic [ref=e66]: Thapar Institute of Engineering & Technology
      - generic [ref=e67]:
        - textbox "Search courses and students" [ref=e72]:
          - /placeholder: Search courses, students…
        - button "Notifications" [ref=e74] [cursor=pointer]
        - button "Open profile menu" [ref=e79] [cursor=pointer]:
          - generic [ref=e80]: AM
    - main [ref=e83]:
      - generic [ref=e84]:
        - generic [ref=e85]:
          - paragraph [ref=e86]: FACULTY WORKSPACE
          - heading "Assessments" [level=1] [ref=e87]
          - paragraph [ref=e88]: Create meaningful assessments and support student progress.
        - link "Dashboard" [ref=e89] [cursor=pointer]:
          - /url: /faculty/dashboard
      - generic [ref=e92]:
        - generic [ref=e93]:
          - generic [ref=e94]:
            - heading "Assessments" [level=2] [ref=e96]
            - generic [ref=e97]:
              - generic [ref=e98]:
                - generic [ref=e99]: Data Structures · Assignment
                - heading "DSA problem set" [level=3] [ref=e100]
                - paragraph [ref=e101]: Due 13/11/2026, 11:59:00 pm
              - generic [ref=e102]: Draft
              - button "Publish results" [ref=e103] [cursor=pointer]
            - generic [ref=e104]:
              - generic [ref=e105]:
                - generic [ref=e106]: Data Structures · Quiz
                - heading "Practice quiz 1" [level=3] [ref=e107]
                - paragraph [ref=e108]: Due 14/11/2026, 11:59:00 pm
              - generic [ref=e109]: Draft
              - button "Publish results" [ref=e110] [cursor=pointer]
            - generic [ref=e111]:
              - generic [ref=e112]:
                - generic [ref=e113]: Data Structures Lab · Quiz
                - heading "Practice quiz 2" [level=3] [ref=e114]
                - paragraph [ref=e115]: Due 15/11/2026, 11:59:00 pm
              - generic [ref=e116]: Draft
              - button "Publish results" [ref=e117] [cursor=pointer]
          - generic [ref=e118]:
            - heading "Student submissions" [level=2] [ref=e120]
            - article [ref=e121]:
              - heading "DSA problem set" [level=3] [ref=e122]
              - generic [ref=e123]: Demo Student
              - paragraph [ref=e124]: Synthetic example submission for grading.
              - generic [ref=e125]:
                - generic [ref=e126]:
                  - text: Marks / 100
                  - spinbutton "Marks / 100" [ref=e127]
                - button "Save grade" [ref=e128] [cursor=pointer]
        - generic [ref=e129]:
          - heading "Create assessment" [level=2] [ref=e131]
          - generic [ref=e133]:
            - generic [ref=e134]:
              - text: Course
              - combobox "Course" [ref=e135]:
                - option "Data Structures · CSE 3A" [selected]
                - option "Data Structures · CSE 3B"
                - option "Data Structures Lab · CSE 3A"
            - generic [ref=e136]:
              - text: Title
              - textbox "Title" [active] [ref=e137]:
                - /placeholder: e.g. Data structures problem set
                - text: QA assignment 4b62d3
            - generic [ref=e138]:
              - text: Type
              - combobox "Type" [ref=e139]:
                - option "Assignment" [selected]
                - option "Quiz"
            - generic [ref=e140]:
              - text: Deadline
              - textbox "Deadline" [ref=e141]
            - button "Create assessment" [ref=e142] [cursor=pointer]
```

# Test source

```ts
  313 |   )
  314 |     await page
  315 |       .getByRole("button", { name: "Open search", exact: true })
  316 |       .click();
  317 |   await page
  318 |     .getByRole("textbox", { name: "Search courses and students" })
  319 |     .fill("Data");
  320 |   await expect(page.locator(".search-results a")).not.toHaveCount(0);
  321 |   await page.locator(".search-results a").first().click();
  322 |   await expect(page).toHaveURL(/faculty\/courses/);
  323 |   await nav(page, "Dashboard");
  324 |   await page
  325 |     .getByRole("button", { name: "Notifications", exact: true })
  326 |     .click();
  327 |   await page.locator(".notifications>button").first().click();
  328 |   await expect(page.locator(".notifications>button").first()).toContainText(
  329 |     "Read",
  330 |   );
  331 |   await page.keyboard.press("Escape");
  332 |   for (const [name, path] of [
  333 |     ["Take Attendance", "attendance"],
  334 |     ["Create Assessment", "assessments"],
  335 |     ["View Reports", "reports"],
  336 |     ["Manage Courses", "courses"],
  337 |   ]) {
  338 |     await page.getByRole("button", { name, exact: true }).click();
  339 |     await expect(page).toHaveURL(new RegExp("/faculty/" + path + "$"));
  340 |     await nav(page, "Dashboard");
  341 |   }
  342 | });
  343 | 
  344 | test("message sending and persisted conversation history", async ({
  345 |   facultyPage: page,
  346 | }) => {
  347 |   await nav(page, "Messages");
  348 |   const body = "Browser QA message " + randomBytes(4).toString("hex");
  349 |   await page.getByRole("textbox", { name: "Message", exact: true }).fill(body);
  350 |   await page.getByRole("button", { name: "Send message", exact: true }).click();
  351 |   await expect(page.getByRole("status")).toContainText("Message sent");
  352 |   await expect(page.locator(".message-history")).toContainText(body);
  353 |   await page.reload();
  354 |   await expect(page.locator(".message-history")).toContainText(body);
  355 | });
  356 | 
  357 | test("course information, file upload/download, announcement and student roster", async ({
  358 |   facultyPage: page,
  359 | }) => {
  360 |   await nav(page, "Courses & LMS");
  361 |   const text = "QA course description " + randomBytes(3).toString("hex");
  362 |   await page.getByLabel("Description", { exact: true }).fill(text);
  363 |   await page.getByRole("button", { name: "Save course", exact: true }).click();
  364 |   await expect(page.getByRole("status")).toContainText("Course updated");
  365 |   await page.reload();
  366 |   await expect(page.getByLabel("Description", { exact: true })).toHaveValue(
  367 |     text,
  368 |   );
  369 |   await page
  370 |     .getByLabel("Learning resource file")
  371 |     .setInputFiles({
  372 |       name: "qa-learning-resource.txt",
  373 |       mimeType: "text/plain",
  374 |       buffer: Buffer.from("Synthetic browser QA learning resource"),
  375 |     });
  376 |   await page.getByRole("button", { name: "Upload", exact: true }).click();
  377 |   await expect(page.getByRole("status")).toContainText("Resource uploaded");
  378 |   const [download] = await Promise.all([
  379 |     page.waitForEvent("download"),
  380 |     page
  381 |       .getByRole("link", { name: "Download qa-learning-resource.txt" })
  382 |       .click(),
  383 |   ]);
  384 |   expect(await download.failure()).toBeNull();
  385 |   const title = "QA announcement " + randomBytes(3).toString("hex");
  386 |   await page.getByLabel("Announcement", { exact: true }).fill(title);
  387 |   await page.getByRole("button", { name: "Publish announcement" }).click();
  388 |   await expect(page.getByRole("status")).toContainText("Announcement shared");
  389 |   await page.reload();
  390 |   await expect(page.locator(".resource-row")).toContainText([title]);
  391 |   await nav(page, "Student Roster");
  392 |   await page
  393 |     .getByRole("textbox", { name: "Search students" })
  394 |     .fill("SYNTH-001");
  395 |   await expect(page.locator("tbody tr")).not.toHaveCount(0);
  396 |   await page
  397 |     .getByRole("textbox", { name: "Search students" })
  398 |     .fill("no-such-student");
  399 |   await expect(
  400 |     page.getByText("No students match this selection."),
  401 |   ).toBeVisible();
  402 |   await page.getByRole("textbox", { name: "Search students" }).clear();
  403 |   await page.getByLabel("Filter by course").selectOption("");
  404 |   await expect(page.locator("tbody tr")).toHaveCount(3);
  405 | });
  406 | 
  407 | test("assessment creation, grading and publication persist", async ({
  408 |   facultyPage: page,
  409 | }) => {
  410 |   await nav(page, "Assessments");
  411 |   const title = "QA assignment " + randomBytes(3).toString("hex");
  412 |   await page.getByLabel("Title", { exact: true }).fill(title);
> 413 |   await page.getByLabel("Type", { exact: true }).selectOption("Assignment");
      |                                                  ^ Error: locator.selectOption: Test timeout of 60000ms exceeded.
  414 |   await page.getByLabel("Deadline", { exact: true }).fill("2099-12-01T12:00");
  415 |   await page
  416 |     .getByRole("button", { name: "Create assessment", exact: true })
  417 |     .click();
  418 |   await expect(page.getByRole("status")).toContainText("Assessment created");
  419 |   await page.reload();
  420 |   const assessment = page
  421 |     .locator(".assessment-item")
  422 |     .filter({ hasText: title });
  423 |   await expect(assessment).toBeVisible();
  424 |   await assessment.getByRole("button", { name: "Publish results" }).click();
  425 |   await expect(assessment).toContainText("Published");
  426 |   await page.getByLabel("Marks / 100").first().fill("87");
  427 |   await page.getByRole("button", { name: "Save grade" }).first().click();
  428 |   await expect(page.getByRole("status")).toContainText("Grade saved");
  429 |   await page.reload();
  430 |   await expect(page.getByLabel("Marks / 100").first()).toHaveValue("87");
  431 | });
  432 | 
  433 | test("attendance QR, student claim and attendance record persistence", async ({
  434 |   facultyPage: page,
  435 |   playwright,
  436 |   baseURL,
  437 | }) => {
  438 |   await nav(page, "Attendance");
  439 |   await page.getByLabel("Class session").selectOption({ index: 0 });
  440 |   await page.getByRole("button", { name: "Generate attendance QR" }).click();
  441 |   await expect(
  442 |     page.getByRole("img", {
  443 |       name: "Short-lived attendance QR for authenticated enrolled students",
  444 |     }),
  445 |   ).toBeVisible();
  446 |   await expect(page.locator(".attendance-token")).toContainText("Active");
  447 |   const sessions = await (
  448 |     await page.request.get("/api/v1/faculty/schedule")
  449 |   ).json();
  450 |   const tokenRes = await page.request.get("/api/v1/auth/csrf");
  451 |   const tokenCsrf = await tokenRes.json();
  452 |   const qr = await page.request.post("/api/v1/faculty/attendance/sessions", {
  453 |     headers: { [tokenCsrf.headerName]: tokenCsrf.token },
  454 |     data: { sessionId: sessions[0].id },
  455 |   });
  456 |   expect(qr.status()).toBe(200);
  457 |   const { token } = await qr.json();
  458 |   const student = await playwright.request.newContext({
  459 |     baseURL,
  460 |     storageState: studentState,
  461 |   });
  462 |   const csrf = await (await student.get("/api/v1/auth/csrf")).json();
  463 |   const claim = await student.post("/api/v1/student/attendance/claim", {
  464 |     headers: { [csrf.headerName]: csrf.token },
  465 |     data: { token },
  466 |   });
  467 |   expect(claim.status()).toBe(200);
  468 |   const duplicate = await student.post("/api/v1/student/attendance/claim", {
  469 |     headers: { [csrf.headerName]: csrf.token },
  470 |     data: { token },
  471 |   });
  472 |   expect(duplicate.status()).toBe(409);
  473 |   await student.dispose();
  474 |   await page.reload();
  475 |   await expect(page.locator("tbody")).toContainText("Demo Student");
  476 | });
  477 | 
  478 | test("settings profile and notification preference persistence", async ({
  479 |   facultyPage: page,
  480 | }) => {
  481 |   await nav(page, "Settings");
  482 |   const name = "Dr. Aarya Mehta (Demo)";
  483 |   await page.getByLabel("Full name").fill(name);
  484 |   await page.getByLabel("Enable notification preferences").uncheck();
  485 |   await page.getByRole("button", { name: "Save changes", exact: true }).click();
  486 |   await expect(page.getByRole("status")).toContainText("Profile updated");
  487 |   await page.reload();
  488 |   await expect(page.getByLabel("Full name")).toHaveValue(name);
  489 |   await expect(
  490 |     page.getByLabel("Enable notification preferences"),
  491 |   ).not.toBeChecked();
  492 | });
  493 | 
  494 | test("student route and server-side faculty access restrictions", async ({
  495 |   page,
  496 | }) => {
  497 |   await page.goto("/faculty/dashboard");
  498 |   await expect(page).toHaveURL(/login\/faculty/);
  499 |   await page.context().addCookies(studentState.cookies);
  500 |   await page.goto("/faculty/dashboard");
  501 |   await expect(page).toHaveURL(/student\/pending/);
  502 |   await expect(page.getByRole("heading")).toContainText("You?re signed in");
  503 |   const res = await page.request.get("/api/v1/faculty/students");
  504 |   expect(res.status()).toBe(403);
  505 |   const noCsrf = await page.request.post("/api/v1/student/attendance/claim", {
  506 |     data: { token: "synthetic-invalid-token" },
  507 |   });
  508 |   expect(noCsrf.status()).toBe(403);
  509 | });
  510 | 
  511 | test("faculty logout invalidates the session and protects routes", async ({
  512 |   page,
  513 | }) => {
```