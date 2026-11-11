# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: academic.spec.ts >> course information, file upload/download, announcement and student roster
- Location: e2e\academic.spec.ts:357:1

# Error details

```
Test timeout of 60000ms exceeded.
```

```
Error: locator.fill: Test timeout of 60000ms exceeded.
Call log:
  - waiting for getByLabel('Description', { exact: true })

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
      - link "Courses & LMS" [active] [ref=e25] [cursor=pointer]:
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
          - heading "Courses & LMS" [level=1] [ref=e87]
          - paragraph [ref=e88]: Your teaching, resources, and announcements in one place.
        - link "Dashboard" [ref=e89] [cursor=pointer]:
          - /url: /faculty/dashboard
      - generic [ref=e92]:
        - generic [ref=e93]:
          - button "DEMO-CS201 · CSE 3A Data Structures 1 enrolled students" [ref=e94] [cursor=pointer]:
            - generic [ref=e97]: DEMO-CS201 · CSE 3A
            - heading "Data Structures" [level=2] [ref=e98]
            - paragraph [ref=e99]: 1 enrolled students
          - button "DEMO-CS202 · CSE 3B Data Structures 1 enrolled students" [ref=e100] [cursor=pointer]:
            - generic [ref=e103]: DEMO-CS202 · CSE 3B
            - heading "Data Structures" [level=2] [ref=e104]
            - paragraph [ref=e105]: 1 enrolled students
          - button "DEMO-CS203 · CSE 3A Data Structures Lab 1 enrolled students" [ref=e106] [cursor=pointer]:
            - generic [ref=e109]: DEMO-CS203 · CSE 3A
            - heading "Data Structures Lab" [level=2] [ref=e110]
            - paragraph [ref=e111]: 1 enrolled students
        - generic [ref=e112]:
          - generic [ref=e113]:
            - heading "Course information" [level=2] [ref=e115]
            - generic [ref=e116]:
              - generic [ref=e117]:
                - text: Course title
                - textbox "Course title" [ref=e118]: Data Structures
              - generic [ref=e119]:
                - text: Description
                - textbox "Description" [ref=e120]: Synthetic local development course
              - button "Save course" [ref=e121] [cursor=pointer]
          - generic [ref=e122]:
            - heading "Learning resources" [level=2] [ref=e124]
            - generic [ref=e125]:
              - button "Learning resource file" [ref=e126]
              - button "Upload" [ref=e127] [cursor=pointer]
            - generic [ref=e131]: No resources shared yet.
          - generic [ref=e132]:
            - heading "Create announcement" [level=2] [ref=e134]
            - generic [ref=e135]:
              - generic [ref=e136]:
                - text: Announcement
                - textbox "Announcement" [ref=e137]:
                  - /placeholder: What should your students know?
              - button "Publish announcement" [ref=e138] [cursor=pointer]
```

# Test source

```ts
  262 |       .isVisible()
  263 |   ) {
  264 |     await page
  265 |       .getByRole("button", { name: "Open navigation", exact: true })
  266 |       .click();
  267 |     await expect(
  268 |       page.getByRole("navigation", { name: "Faculty navigation" }),
  269 |     ).toBeVisible();
  270 |     await page.keyboard.press("Escape");
  271 |     await expect(page.locator(".app-shell")).not.toHaveClass(/mobile-open/);
  272 |   } else {
  273 |     await page.getByRole("button", { name: "Toggle sidebar" }).click();
  274 |     await expect(page.locator(".app-shell")).toHaveClass(/sidebar-collapsed/);
  275 |     await page.getByRole("button", { name: "Toggle sidebar" }).click();
  276 |   }
  277 | });
  278 | 
  279 | test("calendar month navigation, date selection and persisted academic event", async ({
  280 |   facultyPage: page,
  281 | }) => {
  282 |   const month = await page.locator(".month-heading h2").textContent();
  283 |   await page.getByRole("button", { name: "Next month" }).click();
  284 |   await expect(page.locator(".month-heading h2")).not.toHaveText(month!);
  285 |   await page.getByRole("button", { name: "Previous month" }).click();
  286 |   await expect(page.locator(".month-heading h2")).toHaveText(month!);
  287 |   const date = await page
  288 |     .locator(".calendar-grid button")
  289 |     .nth(2)
  290 |     .getAttribute("aria-label");
  291 |   await page.getByRole("button", { name: date!, exact: true }).click();
  292 |   await expect(
  293 |     page.getByRole("button", { name: date!, exact: true }),
  294 |   ).toHaveAttribute("aria-pressed", "true");
  295 |   await nav(page, "Calendar");
  296 |   const title = "QA academic event " + randomBytes(3).toString("hex");
  297 |   await page.getByLabel("Event title").fill(title);
  298 |   await page.getByLabel("Date", { exact: true }).fill(date!);
  299 |   await page.getByRole("button", { name: "Add event", exact: true }).click();
  300 |   await expect(page.getByRole("status")).toContainText("Event added");
  301 |   await page.reload();
  302 |   await page.getByRole("button", { name: date!, exact: true }).click();
  303 |   await expect(page.locator(".calendar-events")).toContainText(title);
  304 | });
  305 | 
  306 | test("search, notification read and quick access links", async ({
  307 |   facultyPage: page,
  308 | }) => {
  309 |   if (
  310 |     await page
  311 |       .getByRole("button", { name: "Open search", exact: true })
  312 |       .isVisible()
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
> 362 |   await page.getByLabel("Description", { exact: true }).fill(text);
      |                                                         ^ Error: locator.fill: Test timeout of 60000ms exceeded.
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
  413 |   await page.getByLabel("Type", { exact: true }).selectOption("Assignment");
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
```