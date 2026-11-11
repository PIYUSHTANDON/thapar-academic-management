# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: academic.spec.ts >> student route and server-side faculty access restrictions
- Location: e2e\academic.spec.ts:494:1

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: getByRole('heading')
Expected substring: "You?re signed in"
Received string:    "You’re signed in, Demo Student."
Timeout: 10000ms

Call log:
  - Expect "toContainText" getByRole('heading') with timeout 10000ms
  - waiting for getByRole('heading')
    23 × locator resolved to <h1>You’re signed in, Demo Student.</h1>
       - unexpected value "You’re signed in, Demo Student."

```

```yaml
- heading "You’re signed in, Demo Student." [level=1]
```

# Test source

```ts
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
> 502 |   await expect(page.getByRole("heading")).toContainText("You?re signed in");
      |                                           ^ Error: expect(locator).toContainText(expected) failed
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
  514 |   await uiLogin(page);
  515 |   await page.getByRole("button", { name: "Open profile menu" }).click();
  516 |   await page.getByRole("button", { name: "Sign out", exact: true }).click();
  517 |   await expect(page).toHaveURL(/\/$/);
  518 |   expect((await page.request.get("/api/v1/auth/me")).status()).toBe(401);
  519 |   await page.goto("/faculty/dashboard");
  520 |   await expect(page).toHaveURL(/login\/faculty/);
  521 | });
  522 | 
  523 | 
  524 | test('dashboard loading state, clear API failure and successful retry',async({page},info)=>{
  525 |  info.annotations.push({type:'expected-http',description:'503'});
  526 |  await page.context().addCookies(facultyState.cookies);
  527 |  let release!:()=>void;const gate=new Promise<void>(r=>release=r);
  528 |  await page.route('**/api/v1/faculty/dashboard',async route=>{await gate;await route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'QA temporary service outage'})});});
  529 |  await page.goto('/faculty/dashboard');await expect(page.getByRole('status')).toContainText('Loading your faculty workspace');release();await expect(page.getByRole('alert')).toContainText('QA temporary service outage');await page.unroute('**/api/v1/faculty/dashboard');await page.getByRole('button',{name:'Retry loading'}).click();await expect(page.getByRole('alert')).toHaveCount(0);await expect(page.locator('.stat-card strong').first()).toHaveText('03');
  530 | });
  531 | 
  532 | test('password validation, password change and reauthentication',async({page},info)=>{
  533 |  info.annotations.push({type:'expected-http',description:'400'});await uiLogin(page);await nav(page,'Settings');
  534 |  const next=randomBytes(24).toString('base64url');await page.getByLabel('Current password',{exact:true}).fill(randomBytes(16).toString('hex'));await page.getByLabel('New password',{exact:true}).fill(next);await page.getByRole('button',{name:'Update password'}).click();await expect(page.getByRole('alert')).toContainText('Current password is incorrect');
  535 |  await page.getByLabel('Current password',{exact:true}).fill(env.QA_FACULTY_PASSWORD!);await page.getByRole('button',{name:'Update password'}).click();await expect(page).toHaveURL(/login\/faculty/);
  536 |  await page.getByLabel('Employee ID or Official Email',{exact:true}).fill(env.QA_FACULTY_ID!);await page.getByLabel('Password',{exact:true}).fill(next);await page.getByRole('button',{name:'Sign In',exact:true}).click();await expect(page).toHaveURL(/faculty\/dashboard/);
  537 |  const csrf=await(await page.request.get('/api/v1/auth/csrf')).json();const restored=await page.request.put('/api/v1/faculty/settings/password',{headers:{[csrf.headerName]:csrf.token},data:{currentPassword:next,newPassword:env.QA_FACULTY_PASSWORD}});expect(restored.status()).toBe(200);
  538 | });
  539 | 
```