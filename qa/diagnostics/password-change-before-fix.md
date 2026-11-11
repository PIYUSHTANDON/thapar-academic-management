# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: academic.spec.ts >> password validation, password change and reauthentication
- Location: e2e\academic.spec.ts:554:1

# Error details

```
Error: expect(page).toHaveURL(expected) failed

Expected pattern: /faculty\/dashboard/
Received string:  "http://127.0.0.1:5174/login/faculty"
Timeout: 10000ms

Call log:
  - Expect "toHaveURL" with timeout 10000ms
    23 × locator resolved to <html lang="en">…</html>
       - unexpected value "http://127.0.0.1:5174/login/faculty"

```

```yaml
- main:
  - img "Thapar Institute of Engineering & Technology"
  - paragraph: Academic Management System
  - heading "Faculty Portal" [level=1]
  - paragraph: Academic Management System — Faculty Access
  - paragraph:
    - text: A legacy of excellence.
    - strong: A future of possibilities.
  - link "Choose another portal":
    - /url: /
  - img "Thapar logo"
  - strong: Academic Management System
  - paragraph: Thapar Institute of Engineering & Technology
  - paragraph: YOUR CAMPUS WORKSPACE
  - heading "Welcome Back, Faculty" [level=2]:
    - text: Welcome Back,
    - emphasis: Faculty
  - paragraph: Sign in to manage courses, attendance, assessments, and student records.
  - text: Employee ID or Official Email
  - textbox "Employee ID or Official Email": qa-faculty-03f02eaa91
  - text: Password
  - textbox "Password": [REDACTED]
    - text: rPA2QI_W2DoSau9NvktjJ9by7aMMW3GB
  - button "Show password"
  - checkbox "Remember me (8 hours)"
  - text: Remember me (8 hours)
  - button "Forgot Password?"
  - alert: You do not have access to this resource.
  - button "Sign In"
  - text: Secure institute access Messages Attendance Courses & LMS Calendar Student Roster Settings
  - paragraph: For authorized faculty members only.
```

```
Error: Unexpected failed HTTP responses

expect(received).toEqual(expected) // deep equality

- Expected  -  1
+ Received  + 14

- Array []
+ Array [
+   Object {
+     "path": "/api/v1/faculty/courses",
+     "status": 401,
+   },
+   Object {
+     "path": "/api/v1/faculty/students",
+     "status": 401,
+   },
+   Object {
+     "path": "/api/v1/auth/login",
+     "status": 403,
+   },
+ ]
```

# Test source

```ts
  1   | import {
  2   |   test as base,
  3   |   expect,
  4   |   type Page,
  5   |   type BrowserContext,
  6   |   type APIRequestContext,
  7   | } from "@playwright/test";
  8   | import { randomBytes } from "node:crypto";
  9   | import { resolve } from "node:path";
  10  | import { mkdirSync } from "node:fs";
  11  | const env = process.env;
  12  | for (const name of [
  13  |   "QA_FACULTY_ID",
  14  |   "QA_FACULTY_PASSWORD",
  15  |   "QA_STUDENT_ID",
  16  |   "QA_STUDENT_PASSWORD",
  17  | ])
  18  |   if (!env[name])
  19  |     throw Error(
  20  |       `Missing ${name}; run npm run test:e2e to generate ephemeral environment credentials.`,
  21  |     );
  22  | let facultyState: any, studentState: any;
  23  | const test = base.extend<{ facultyPage: Page }>({
  24  |   page: async ({ page }, use, info) => {
  25  |     const errors: string[] = [],
  26  |       http: { path: string; status: number }[] = [],
  27  |       failed: string[] = [];
  28  |     page.on("pageerror", (e) => errors.push(e.message));
  29  |     page.on("console", (m) => {
  30  |       if (m.type() === "error") errors.push(m.text());
  31  |     });
  32  |     page.on("response", (r) => {
  33  |       if (r.status() >= 400)
  34  |         http.push({ path: new URL(r.url()).pathname, status: r.status() });
  35  |     });
  36  |     page.on("requestfailed", (r) => {
  37  |       if (!r.failure()?.errorText.includes("ERR_ABORTED"))
  38  |         failed.push(new URL(r.url()).pathname + ": " + r.failure()?.errorText);
  39  |     });
  40  |     await use(page);
  41  |     const allowed = info.annotations
  42  |       .filter((a) => a.type === "expected-http")
  43  |       .map((a) => Number(a.description));
  44  |     const unexpectedHttp = http.filter((r) => !allowed.includes(r.status));
  45  |     const unexpectedErrors = errors.filter(
  46  |       (e) => !allowed.some((s) => e.includes(`status of ${s}`)),
  47  |     );
  48  |     await info.attach("browser-network-audit", {
  49  |       body: JSON.stringify({ http, errors, failed }),
  50  |       contentType: "application/json",
  51  |     });
> 52  |     expect(unexpectedHttp, "Unexpected failed HTTP responses").toEqual([]);
      |                                                                ^ Error: Unexpected failed HTTP responses
  53  |     expect(unexpectedErrors, "Browser console/page errors").toEqual([]);
  54  |     expect(failed, "Failed network requests").toEqual([]);
  55  |   },
  56  |   facultyPage: async ({ page }, use) => {
  57  |     await page.context().addCookies(facultyState.cookies);
  58  |     await page.goto("/faculty/dashboard");
  59  |     await expect(
  60  |       page.getByRole("heading", { name: "Quick Access" }),
  61  |     ).toBeVisible();
  62  |     await expect(page.locator(".loading")).toHaveCount(0);
  63  |     await use(page);
  64  |   },
  65  | });
  66  | async function apiLogin(
  67  |   request: APIRequestContext,
  68  |   role: "FACULTY" | "STUDENT",
  69  | ) {
  70  |   const csrf = await (await request.get("/api/v1/auth/csrf")).json();
  71  |   const res = await request.post("/api/v1/auth/login", {
  72  |     headers: { [csrf.headerName]: csrf.token },
  73  |     data: {
  74  |       identifier: env[role === "FACULTY" ? "QA_FACULTY_ID" : "QA_STUDENT_ID"],
  75  |       password:
  76  |         env[role === "FACULTY" ? "QA_FACULTY_PASSWORD" : "QA_STUDENT_PASSWORD"],
  77  |       role,
  78  |     },
  79  |   });
  80  |   expect(res.status()).toBe(200);
  81  |   return request.storageState();
  82  | }
  83  | async function uiLogin(page: Page, role: "faculty" | "student" = "faculty") {
  84  |   await page.goto("/login/" + role);
  85  |   await page
  86  |     .getByLabel(
  87  |       role === "faculty"
  88  |         ? "Employee ID or Official Email"
  89  |         : "Enrollment Number or Official Email",
  90  |       { exact: true },
  91  |     )
  92  |     .fill(env[role === "faculty" ? "QA_FACULTY_ID" : "QA_STUDENT_ID"]!);
  93  |   await page
  94  |     .getByLabel("Password", { exact: true })
  95  |     .fill(
  96  |       env[role === "faculty" ? "QA_FACULTY_PASSWORD" : "QA_STUDENT_PASSWORD"]!,
  97  |     );
  98  |   await page.getByRole("button", { name: "Sign In", exact: true }).click();
  99  |   await expect(page).toHaveURL(
  100 |     role === "faculty" ? /faculty\/dashboard/ : /student\/pending/,
  101 |   );
  102 | }
  103 | async function nav(page: Page, name: string) {
  104 |   if (
  105 |     await page
  106 |       .getByRole("button", { name: "Open navigation", exact: true })
  107 |       .isVisible()
  108 |   )
  109 |     await page
  110 |       .getByRole("button", { name: "Open navigation", exact: true })
  111 |       .click();
  112 |   await page
  113 |     .getByRole("navigation", { name: "Faculty navigation" })
  114 |     .getByRole("link", { name, exact: true })
  115 |     .click();
  116 |   await expect(page.locator(".loading")).toHaveCount(0);
  117 | }
  118 | async function noOverflow(page: Page) {
  119 |   await expect
  120 |     .poll(
  121 |       () =>
  122 |         page.evaluate(() => document.documentElement.scrollWidth - innerWidth),
  123 |       { message: "Page-wide horizontal overflow" },
  124 |     )
  125 |     .toBeLessThanOrEqual(1);
  126 | }
  127 | async function screenshot(page: Page, name: string, project: string) {
  128 |   mkdirSync(resolve("../qa/screenshots"), { recursive: true });
  129 |   await page.screenshot({
  130 |     path: resolve(`../qa/screenshots/${project}-${name}.png`),
  131 |     fullPage: true,
  132 |   });
  133 | }
  134 | test.beforeAll(async ({ playwright, baseURL }) => {
  135 |   const faculty = await playwright.request.newContext({ baseURL });
  136 |   facultyState = await apiLogin(faculty, "FACULTY");
  137 |   await faculty.dispose();
  138 |   const student = await playwright.request.newContext({ baseURL });
  139 |   studentState = await apiLogin(student, "STUDENT");
  140 |   await student.dispose();
  141 | });
  142 | 
  143 | test("main localhost, role selection and branded login", async ({
  144 |   page,
  145 | }, info) => {
  146 |   await page.goto("http://127.0.0.1:5173/");
  147 |   await expect(
  148 |     page.getByRole("link", { name: /Faculty Portal/ }),
  149 |   ).toBeVisible();
  150 |   expect(
  151 |     (await page.request.get("http://127.0.0.1:5173/api/v1/health")).status(),
  152 |   ).toBe(200);
```