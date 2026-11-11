# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: student.spec.ts >> faculty material and announcements appear in student LMS after refresh
- Location: e2e\student.spec.ts:386:1

# Error details

```
Test timeout of 60000ms exceeded.
```

```
Error: page.waitForEvent: Test timeout of 60000ms exceeded.
=========================== logs ===========================
waiting for event "download"
============================================================
```

# Test source

```ts
  311 |   await teacher
  312 |     .getByRole("button", { name: "Create assessment", exact: true })
  313 |     .click();
  314 |   await expect(teacher.locator(".toast")).toContainText("Assessment created");
  315 |   await nav(page, "Assessments");
  316 |   let panel = page
  317 |     .locator(".student-assessments>section")
  318 |     .filter({ hasText: title });
  319 |   await expect(panel).toBeVisible();
  320 |   await panel
  321 |     .getByRole("textbox", { name: "Submission text", exact: true })
  322 |     .fill("Synthetic synchronized submission");
  323 |   await panel.getByLabel("Assignment file", { exact: true }).setInputFiles({
  324 |     name: "student-work.txt",
  325 |     mimeType: "text/plain",
  326 |     buffer: Buffer.from("Synthetic student attachment"),
  327 |   });
  328 |   await panel
  329 |     .getByRole("button", { name: "Submit assignment", exact: true })
  330 |     .click();
  331 |   await expect(page.locator(".toast")).toContainText("Assignment submitted");
  332 |   await page.reload();
  333 |   await expect(panel).toContainText("student-work.txt");
  334 |   await teacher.reload();
  335 |   const sub = teacher.locator(".submission").filter({ hasText: title });
  336 |   const [download] = await Promise.all([
  337 |     teacher.waitForEvent("download"),
  338 |     sub.getByRole("link", { name: "Download student-work.txt" }).click(),
  339 |   ]);
  340 |   expect(await download.failure()).toBeNull();
  341 |   await sub.getByLabel("Marks / 100").fill("87");
  342 |   await sub
  343 |     .getByRole("textbox", { name: "Faculty feedback", exact: true })
  344 |     .fill("Thoughtful work with clear reasoning.");
  345 |   await sub.getByRole("button", { name: "Save grade", exact: true }).click();
  346 |   await expect(teacher.locator(".toast")).toContainText("Grade saved");
  347 |   await nav(page, "Results");
  348 |   await expect(page.locator("tbody")).not.toContainText(title);
  349 |   const assessment = teacher
  350 |     .locator(".assessment-item")
  351 |     .filter({ hasText: title });
  352 |   await assessment
  353 |     .getByRole("button", { name: "Publish results", exact: true })
  354 |     .click();
  355 |   await expect(teacher.locator(".toast")).toContainText("Results published");
  356 |   await page.reload();
  357 |   const row = page.locator("tbody tr").filter({ hasText: title });
  358 |   await expect(row).toContainText("87");
  359 |   await expect(row).toContainText("Thoughtful work with clear reasoning.");
  360 |   expect(
  361 |     (await (await student.get("/api/v1/student/dashboard")).json()).currentCgpa,
  362 |   ).toBeNull();
  363 | });
  364 | 
  365 | test("student and faculty messages synchronize in both directions", async ({
  366 |   studentPage: page,
  367 |   teacherPage: teacher,
  368 | }) => {
  369 |   const text = "Student question " + randomBytes(3).toString("hex"),
  370 |     reply = "Faculty reply " + randomBytes(3).toString("hex");
  371 |   await nav(page, "Messages");
  372 |   await page.getByRole("textbox", { name: "Message", exact: true }).fill(text);
  373 |   await page.getByRole("button", { name: "Send message" }).click();
  374 |   await expect(page.locator(".toast")).toContainText("Message sent");
  375 |   await nav(teacher, "Messages", "Faculty");
  376 |   await expect(teacher.locator(".message-history")).toContainText(text);
  377 |   await teacher
  378 |     .getByRole("textbox", { name: "Message", exact: true })
  379 |     .fill(reply);
  380 |   await teacher.getByRole("button", { name: "Send message" }).click();
  381 |   await expect(teacher.locator(".toast")).toContainText("Message sent");
  382 |   await page.reload();
  383 |   await expect(page.locator(".message-history")).toContainText(reply);
  384 | });
  385 | 
  386 | test("faculty material and announcements appear in student LMS after refresh", async ({
  387 |   studentPage: page,
  388 |   teacherPage: teacher,
  389 | }) => {
  390 |   await nav(teacher, "Courses & LMS", "Faculty");
  391 |   await teacher.getByLabel("Learning resource file").setInputFiles({
  392 |     name: "shared-notes.txt",
  393 |     mimeType: "text/plain",
  394 |     buffer: Buffer.from("Synthetic shared course notes"),
  395 |   });
  396 |   await teacher.getByRole("button", { name: "Upload", exact: true }).click();
  397 |   await expect(teacher.locator(".toast")).toContainText("Resource uploaded");
  398 |   const title = "Shared notice " + randomBytes(3).toString("hex");
  399 |   await teacher.getByLabel("Announcement", { exact: true }).fill(title);
  400 |   await teacher
  401 |     .getByRole("textbox", { name: "Announcement description", exact: true })
  402 |     .fill("Complete announcement details from the course faculty.");
  403 |   await teacher.getByRole("button", { name: "Publish announcement" }).click();
  404 |   await expect(teacher.locator(".toast")).toContainText("Announcement shared");
  405 |   await nav(page, "Courses & LMS");
  406 |   await expect(page.locator(".resource-row")).toContainText([
  407 |     "shared-notes.txt",
  408 |     title,
  409 |   ]);
  410 |   const [download] = await Promise.all([
> 411 |     page.waitForEvent("download"),
      |          ^ Error: page.waitForEvent: Test timeout of 60000ms exceeded.
  412 |     page.getByRole("link", { name: "Download shared-notes.txt" }).click(),
  413 |   ]);
  414 |   expect(await download.failure()).toBeNull();
  415 |   await page
  416 |     .locator(".resource-row")
  417 |     .filter({ hasText: title })
  418 |     .getByRole("button", { name: "Read announcement" })
  419 |     .click();
  420 |   await expect(page.getByRole("dialog")).toContainText(
  421 |     "Complete announcement details",
  422 |   );
  423 |   await page.getByRole("button", { name: "Close announcement" }).click();
  424 | });
  425 | 
  426 | test("faculty QR image decodes locally, student claims attendance and replay is rejected", async ({
  427 |   studentPage: page,
  428 |   teacherPage: teacher,
  429 | }, info) => {
  430 |   info.annotations.push({ type: "expected-http", description: "409" });
  431 |   await nav(teacher, "Attendance", "Faculty");
  432 |   await teacher.getByLabel("Class session").selectOption({ index: 0 });
  433 |   await teacher.getByRole("button", { name: "Generate attendance QR" }).click();
  434 |   const image = teacher.getByRole("img", {
  435 |     name: "Short-lived attendance QR for authenticated enrolled students",
  436 |   });
  437 |   await expect(image).toBeVisible();
  438 |   const src = await image.getAttribute("src");
  439 |   await nav(page, "Attendance");
  440 |   await page.getByLabel("Attendance QR image").setInputFiles({
  441 |     name: "attendance.png",
  442 |     mimeType: "image/png",
  443 |     buffer: Buffer.from(src!.split(",")[1], "base64"),
  444 |   });
  445 |   await expect(page.locator(".toast")).toContainText("Attendance QR decoded");
  446 |   const token = await page
  447 |     .getByRole("textbox", { name: "Attendance token" })
  448 |     .inputValue();
  449 |   await page.getByRole("button", { name: "Claim attendance" }).click();
  450 |   await expect(page.locator(".toast")).toContainText("Attendance recorded");
  451 |   await teacher.reload();
  452 |   await expect(teacher.locator("tbody")).toContainText("Demo Student");
  453 |   await page.getByRole("textbox", { name: "Attendance token" }).fill(token);
  454 |   await page.getByRole("button", { name: "Claim attendance" }).click();
  455 |   await expect(page.getByRole("alert")).toContainText(
  456 |     "Attendance already recorded",
  457 |   );
  458 |   await page.getByRole("textbox", { name: "Attendance token" }).clear();
  459 | });
  460 | 
  461 | test("calendar events, month navigation and faculty timetable edits synchronize", async ({
  462 |   studentPage: page,
  463 |   teacherPage: teacher,
  464 | }) => {
  465 |   await nav(page, "Calendar");
  466 |   const month = await page.locator(".month-heading h2").textContent();
  467 |   await page.getByRole("button", { name: "Next month" }).click();
  468 |   await expect(page.locator(".month-heading h2")).not.toHaveText(month!);
  469 |   await page.getByRole("button", { name: "Previous month" }).click();
  470 |   await expect(page.locator(".month-heading h2")).toHaveText(month!);
  471 |   await nav(teacher, "Calendar", "Faculty");
  472 |   const title = "Shared academic event " + randomBytes(3).toString("hex"),
  473 |     date = new Intl.DateTimeFormat("en-CA", {
  474 |       timeZone: "Asia/Kolkata",
  475 |     }).format(new Date());
  476 |   await teacher.getByLabel("Event title").fill(title);
  477 |   await teacher.getByLabel("Date", { exact: true }).fill(date);
  478 |   await teacher.getByRole("button", { name: "Add event", exact: true }).click();
  479 |   await expect(teacher.locator(".toast")).toContainText("Event added");
  480 |   await page.reload();
  481 |   await page.getByRole("button", { name: date, exact: true }).click();
  482 |   await expect(page.locator(".calendar-events")).toContainText(title);
  483 |   await teacher
  484 |     .getByRole("combobox", { name: "Teaching session", exact: true })
  485 |     .selectOption({ index: 1 });
  486 |   await teacher.getByLabel("Class room").fill("QA-LT-303");
  487 |   await teacher.getByRole("button", { name: "Save class information" }).click();
  488 |   await expect(
  489 |     teacher.getByText("Class information updated", { exact: true }),
  490 |   ).toBeVisible();
  491 |   expect(
  492 |     JSON.stringify(
  493 |       await (await student.get("/api/v1/student/schedule")).json(),
  494 |     ),
  495 |   ).toContain("QA-LT-303");
  496 |   await page.reload();
  497 |   await expect(page.locator(".calendar-events")).toContainText("QA-LT-303");
  498 | });
  499 | 
  500 | test("role restrictions, upload errors and protected official academic fields", async ({
  501 |   studentPage: page,
  502 | }, info) => {
  503 |   info.annotations.push({ type: "expected-http", description: "400" });
  504 |   expect((await student.get("/api/v1/faculty/students")).status()).toBe(403);
  505 |   expect((await faculty.get("/api/v1/student/dashboard")).status()).toBe(403);
  506 |   expect(
  507 |     (await student.get("/api/v1/student/courses/999999/resources")).status(),
  508 |   ).toBe(403);
  509 |   const r = await mutate(faculty, "/faculty/assessments", {
  510 |     courseId: courses[0].id,
  511 |     title: "Upload validation assessment",
```