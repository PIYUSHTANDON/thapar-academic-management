import {
  test as base,
  expect,
  type Page,
  type BrowserContext,
  type APIRequestContext,
} from "@playwright/test";
import { randomBytes } from "node:crypto";
import { resolve } from "node:path";
import { mkdirSync } from "node:fs";
const env = process.env;
for (const name of [
  "QA_FACULTY_ID",
  "QA_FACULTY_PASSWORD",
  "QA_STUDENT_ID",
  "QA_STUDENT_PASSWORD",
])
  if (!env[name])
    throw Error(
      `Missing ${name}; run npm run test:e2e to generate ephemeral environment credentials.`,
    );
let facultyState: any, studentState: any;
const test = base.extend<{ facultyPage: Page }>({
  page: async ({ page }, use, info) => {
    const errors: string[] = [],
      http: { path: string; status: number }[] = [],
      failed: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("console", (m) => {
      if (m.type() === "error") errors.push(m.text());
    });
    page.on("response", (r) => {
      if (r.status() >= 400)
        http.push({ path: new URL(r.url()).pathname, status: r.status() });
    });
    page.on("requestfailed", (r) => {
      if (!r.failure()?.errorText.includes("ERR_ABORTED"))
        failed.push(new URL(r.url()).pathname + ": " + r.failure()?.errorText);
    });
    await use(page);
    const allowed = info.annotations
      .filter((a) => a.type === "expected-http")
      .map((a) => Number(a.description));
    const unexpectedHttp = http.filter((r) => !allowed.includes(r.status));
    const unexpectedErrors = errors.filter(
      (e) => !allowed.some((s) => e.includes(`status of ${s}`)),
    );
    await info.attach("browser-network-audit", {
      body: JSON.stringify({ http, errors, failed }),
      contentType: "application/json",
    });
    expect(unexpectedHttp, "Unexpected failed HTTP responses").toEqual([]);
    expect(unexpectedErrors, "Browser console/page errors").toEqual([]);
    expect(failed, "Failed network requests").toEqual([]);
  },
  facultyPage: async ({ page }, use) => {
    await page.context().addCookies(facultyState.cookies);
    await page.goto("/faculty/dashboard");
    await expect(
      page.getByRole("heading", { name: "Quick Access" }),
    ).toBeVisible();
    await expect(page.locator(".loading")).toHaveCount(0);
    await use(page);
  },
});
async function apiLogin(
  request: APIRequestContext,
  role: "FACULTY" | "STUDENT",
) {
  const csrf = await (await request.get("/api/v1/auth/csrf")).json();
  const res = await request.post("/api/v1/auth/login", {
    headers: { [csrf.headerName]: csrf.token },
    data: {
      identifier: env[role === "FACULTY" ? "QA_FACULTY_ID" : "QA_STUDENT_ID"],
      password:
        env[role === "FACULTY" ? "QA_FACULTY_PASSWORD" : "QA_STUDENT_PASSWORD"],
      role,
    },
  });
  expect(res.status()).toBe(200);
  return request.storageState();
}
async function uiLogin(page: Page, role: "faculty" | "student" = "faculty") {
  await page.goto("/login/" + role);
  await page
    .getByLabel(
      role === "faculty"
        ? "Employee ID or Official Email"
        : "Enrollment Number or Official Email",
      { exact: true },
    )
    .fill(env[role === "faculty" ? "QA_FACULTY_ID" : "QA_STUDENT_ID"]!);
  await page
    .getByLabel("Password", { exact: true })
    .fill(
      env[role === "faculty" ? "QA_FACULTY_PASSWORD" : "QA_STUDENT_PASSWORD"]!,
    );
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(
    role === "faculty" ? /faculty\/dashboard/ : /student\/dashboard/,
  );
}
async function nav(page: Page, name: string) {
  if (
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .isVisible()
  )
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .click();
  const link = page
    .getByRole("navigation", { name: "Faculty navigation" })
    .getByRole("link", { name, exact: true });
  const href = await link.getAttribute("href");
  const changed = new URL(page.url()).pathname !== href;
  const endpoint = href?.endsWith("/dashboard")
    ? "/api/v1/faculty/dashboard"
    : "/api/v1/faculty/courses";
  const response = changed
    ? page.waitForResponse(
        (r) =>
          new URL(r.url()).pathname === endpoint &&
          r.request().method() === "GET" &&
          r.ok(),
      )
    : Promise.resolve();
  await link.click();
  await response;
  await expect(page.locator(".loading")).toHaveCount(0);
  await expect(page.locator(".app-shell")).not.toHaveClass(/mobile-open/);
  if (
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .isVisible()
  )
    await expect
      .poll(() =>
        page
          .locator(".sidebar")
          .evaluate((e) => e.getBoundingClientRect().right),
      )
      .toBeLessThanOrEqual(1);
}
async function noOverflow(page: Page) {
  await expect
    .poll(
      () =>
        page.evaluate(() => document.documentElement.scrollWidth - innerWidth),
      { message: "Page-wide horizontal overflow" },
    )
    .toBeLessThanOrEqual(1);
}
async function screenshot(page: Page, name: string, project: string) {
  mkdirSync(resolve("../qa/screenshots"), { recursive: true });
  await page.screenshot({
    path: resolve(`../qa/screenshots/${project}-${name}.png`),
    fullPage: true,
    animations: "disabled",
  });
}
test.beforeAll(async ({ playwright, baseURL }) => {
  const faculty = await playwright.request.newContext({ baseURL });
  facultyState = await apiLogin(faculty, "FACULTY");
  await faculty.dispose();
  const student = await playwright.request.newContext({ baseURL });
  studentState = await apiLogin(student, "STUDENT");
  await student.dispose();
});

test("main localhost, role selection and branded login", async ({
  page,
}, info) => {
  await page.goto("http://127.0.0.1:5173/");
  await expect(
    page.getByRole("link", { name: /Faculty Portal/ }),
  ).toBeVisible();
  expect(
    (await page.request.get("http://127.0.0.1:5173/api/v1/health")).status(),
  ).toBe(200);
  await page.goto("/");
  await noOverflow(page);
  await screenshot(page, "roles", info.project.name);
  await page.getByRole("link", { name: /Faculty Portal/ }).click();
  await expect(
    page.getByRole("heading", { name: "Faculty Portal", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByAltText("Thapar Institute of Engineering & Technology"),
  ).toBeVisible();
  await noOverflow(page);
  await screenshot(page, "login", info.project.name);
  await page.getByRole("link", { name: "Choose another portal" }).click();
  await page.getByRole("link", { name: /Student Portal/ }).click();
  await expect(
    page.getByLabel("Enrollment Number or Official Email", { exact: true }),
  ).toBeVisible();
  await noOverflow(page);
});

test("login validation, password visibility, recovery and invalid credentials", async ({
  page,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "401" });
  await page.goto("/login/faculty");
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  expect(
    await page
      .getByLabel("Employee ID or Official Email", { exact: true })
      .evaluate((e: HTMLInputElement) => e.validity.valueMissing),
  ).toBe(true);
  const input = page.getByLabel("Password", { exact: true });
  await input.fill(randomBytes(16).toString("hex"));
  await page.getByRole("button", { name: "Show password" }).click();
  await expect(input).toHaveAttribute("type", "text");
  await page.getByRole("button", { name: "Hide password" }).click();
  await expect(input).toHaveAttribute("type", "password");
  await input.clear();
  await page.getByRole("button", { name: "Forgot Password?" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "Back to sign in" }).click();
  await page
    .getByLabel("Employee ID or Official Email", { exact: true })
    .fill(env.QA_FACULTY_ID!);
  await input.fill(randomBytes(16).toString("hex"));
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Invalid credentials or role",
  );
  await expect(page).toHaveURL(/login\/faculty/);
  await input.clear();
});

test("faculty UI login, session cookie and backend dashboard statistics", async ({
  page,
}, info) => {
  await uiLogin(page);
  await expect(page.locator(".loading")).toHaveCount(0);
  const data = await (
    await page.request.get("/api/v1/faculty/dashboard")
  ).json();
  const stats = page.locator(".stat-card strong");
  await expect(stats).toHaveCount(4);
  await expect(stats.nth(0)).toHaveText(
    String(data.activeClasses).padStart(2, "0"),
  );
  await expect(stats.nth(1)).toHaveText(
    String(data.totalStudents).padStart(2, "0"),
  );
  await expect(stats.nth(2)).toHaveText(data.attendancePercentage + "%");
  await expect(page.locator("tbody tr")).toHaveCount(3);
  await expect(
    page.getByRole("heading", { name: "Action Required" }),
  ).toBeVisible();
  const cookie = (await page.context().cookies()).find(
    (c) => c.name === "JSESSIONID",
  );
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe("Strict");
  await noOverflow(page);
  await screenshot(page, "dashboard", info.project.name);
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Quick Access" }),
  ).toBeVisible();
});

test("all eight sidebar modules and responsive navigation", async ({
  facultyPage: page,
}, info) => {
  for (const [name, path] of [
    ["Dashboard", "dashboard"],
    ["Messages", "messages"],
    ["Attendance", "attendance"],
    ["Courses & LMS", "courses"],
    ["Assessments", "assessments"],
    ["Calendar", "calendar"],
    ["Student Roster", "students"],
    ["Settings", "settings"],
  ]) {
    await nav(page, name);
    await expect(page).toHaveURL(new RegExp("/faculty/" + path + "$"));
    await expect(page.getByRole("main")).toBeVisible();
    await noOverflow(page);
    await screenshot(page, path + "-module", info.project.name);
  }
  if (
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .isVisible()
  ) {
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .click();
    await expect(
      page.getByRole("navigation", { name: "Faculty navigation" }),
    ).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page.locator(".app-shell")).not.toHaveClass(/mobile-open/);
  } else {
    await page.getByRole("button", { name: "Toggle sidebar" }).click();
    await expect(page.locator(".app-shell")).toHaveClass(/sidebar-collapsed/);
    await page.getByRole("button", { name: "Toggle sidebar" }).click();
  }
});

test("calendar month navigation, date selection and persisted academic event", async ({
  facultyPage: page,
}) => {
  const month = await page.locator(".month-heading h2").textContent();
  await page.getByRole("button", { name: "Next month" }).click();
  await expect(page.locator(".month-heading h2")).not.toHaveText(month!);
  await page.getByRole("button", { name: "Previous month" }).click();
  await expect(page.locator(".month-heading h2")).toHaveText(month!);
  const date = await page
    .locator(".calendar-grid button")
    .nth(2)
    .getAttribute("aria-label");
  await page.getByRole("button", { name: date!, exact: true }).click();
  await expect(
    page.getByRole("button", { name: date!, exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await nav(page, "Calendar");
  const title = "QA academic event " + randomBytes(3).toString("hex");
  await page.getByLabel("Event title").fill(title);
  await page.getByLabel("Date", { exact: true }).fill(date!);
  await page.getByRole("button", { name: "Add event", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Event added");
  await page.reload();
  await page.getByRole("button", { name: date!, exact: true }).click();
  await expect(page.locator(".calendar-events")).toContainText(title);
});

test("search, notification read and quick access links", async ({
  facultyPage: page,
}) => {
  if (
    await page
      .getByRole("button", { name: "Open search", exact: true })
      .isVisible()
  )
    await page
      .getByRole("button", { name: "Open search", exact: true })
      .click();
  await page
    .getByRole("textbox", { name: "Search courses and students" })
    .fill("Data");
  await expect(page.locator(".search-results a")).not.toHaveCount(0);
  await page.locator(".search-results a").first().click();
  await expect(page).toHaveURL(/faculty\/courses/);
  await nav(page, "Dashboard");
  await page
    .getByRole("button", { name: "Notifications", exact: true })
    .click();
  await page.locator(".notifications>button").first().click();
  await expect(page.locator(".notifications>button").first()).toContainText(
    "Read",
  );
  await page.keyboard.press("Escape");
  for (const [name, path] of [
    ["Take Attendance", "attendance"],
    ["Create Assessment", "assessments"],
    ["View Reports", "reports"],
    ["Manage Courses", "courses"],
  ]) {
    await page.getByRole("button", { name, exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/faculty/" + path + "$"));
    await nav(page, "Dashboard");
  }
});

test("message sending and persisted conversation history", async ({
  facultyPage: page,
}) => {
  await nav(page, "Messages");
  const body = "Browser QA message " + randomBytes(4).toString("hex");
  await page.getByRole("textbox", { name: "Message", exact: true }).fill(body);
  await page.getByRole("button", { name: "Send message", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Message sent");
  await expect(page.locator(".message-history")).toContainText(body);
  await page.reload();
  await expect(page.locator(".message-history")).toContainText(body);
});

test("course information, file upload/download, announcement and student roster", async ({
  facultyPage: page,
}) => {
  await nav(page, "Courses & LMS");
  const text = "QA course description " + randomBytes(3).toString("hex");
  await page
    .getByRole("textbox", { name: "Description", exact: true })
    .fill(text);
  await page.getByRole("button", { name: "Save course", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Course updated");
  await page.reload();
  await expect(
    page.getByRole("textbox", { name: "Description", exact: true }),
  ).toHaveValue(text);
  await page.getByLabel("Learning resource file").setInputFiles({
    name: "qa-learning-resource.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("Synthetic browser QA learning resource"),
  });
  await page.getByRole("button", { name: "Upload", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Resource uploaded");
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page
      .getByRole("link", { name: "Download qa-learning-resource.txt" })
      .click(),
  ]);
  expect(await download.failure()).toBeNull();
  const title = "QA announcement " + randomBytes(3).toString("hex");
  await page.getByLabel("Announcement", { exact: true }).fill(title);
  await page.getByRole("button", { name: "Publish announcement" }).click();
  await expect(page.getByRole("status")).toContainText("Announcement shared");
  await page.reload();
  await expect(page.locator(".resource-row")).toContainText([title]);
  await nav(page, "Student Roster");
  await page
    .getByRole("textbox", { name: "Search students" })
    .fill("SYNTH-001");
  await expect(page.locator("tbody tr")).not.toHaveCount(0);
  await page
    .getByRole("textbox", { name: "Search students" })
    .fill("no-such-student");
  await expect(
    page.getByText("No students match this selection."),
  ).toBeVisible();
  await page.getByRole("textbox", { name: "Search students" }).clear();
  await page.getByLabel("Filter by course").selectOption("");
  await expect(page.locator("tbody tr")).toHaveCount(3);
});

test("assessment creation, grading and publication persist", async ({
  facultyPage: page,
}) => {
  await nav(page, "Assessments");
  const title = "QA assignment " + randomBytes(3).toString("hex");
  await page.getByLabel("Title", { exact: true }).fill(title);
  await page
    .getByRole("combobox", { name: "Type", exact: true })
    .selectOption("Assignment");
  await page.getByLabel("Deadline", { exact: true }).fill("2099-12-01T12:00");
  await page
    .getByRole("button", { name: "Create assessment", exact: true })
    .click();
  await expect(page.getByRole("status")).toContainText("Assessment created");
  await page.reload();
  const assessment = page
    .locator(".assessment-item")
    .filter({ hasText: title });
  await expect(assessment).toBeVisible();
  await assessment.getByRole("button", { name: "Publish results" }).click();
  await expect(assessment).toContainText("Published");
  await page.getByLabel("Marks / 100").first().fill("87");
  await page.getByRole("button", { name: "Save grade" }).first().click();
  await expect(page.getByRole("status")).toContainText("Grade saved");
  await page.reload();
  await expect(page.getByLabel("Marks / 100").first()).toHaveValue("87");
});

test("attendance QR, student claim and attendance record persistence", async ({
  facultyPage: page,
  playwright,
  baseURL,
}) => {
  await nav(page, "Attendance");
  await page.getByLabel("Class session").selectOption({ index: 0 });
  await page.getByRole("button", { name: "Generate attendance QR" }).click();
  await expect(
    page.getByRole("img", {
      name: "Short-lived attendance QR for authenticated enrolled students",
    }),
  ).toBeVisible();
  await expect(page.locator(".attendance-token")).toContainText("Active");
  const sessions = await (
    await page.request.get("/api/v1/faculty/schedule")
  ).json();
  const tokenRes = await page.request.get("/api/v1/auth/csrf");
  const tokenCsrf = await tokenRes.json();
  const qr = await page.request.post("/api/v1/faculty/attendance/sessions", {
    headers: { [tokenCsrf.headerName]: tokenCsrf.token },
    data: { sessionId: sessions[0].id },
  });
  expect(qr.status()).toBe(200);
  const { token } = await qr.json();
  const student = await playwright.request.newContext({
    baseURL,
    storageState: studentState,
  });
  const csrf = await (await student.get("/api/v1/auth/csrf")).json();
  const claim = await student.post("/api/v1/student/attendance/claim", {
    headers: { [csrf.headerName]: csrf.token },
    data: { token },
  });
  expect(claim.status()).toBe(200);
  const duplicate = await student.post("/api/v1/student/attendance/claim", {
    headers: { [csrf.headerName]: csrf.token },
    data: { token },
  });
  expect(duplicate.status()).toBe(409);
  await student.dispose();
  await page.reload();
  await expect(page.locator("tbody")).toContainText("Demo Student");
});

test("settings profile and notification preference persistence", async ({
  facultyPage: page,
}) => {
  await nav(page, "Settings");
  const name = "Dr. Aarya Mehta (Demo)";
  await page.getByLabel("Full name").fill(name);
  await page.getByLabel("Enable notification preferences").uncheck();
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Profile updated");
  await page.reload();
  await expect(page.getByLabel("Full name")).toHaveValue(name);
  await expect(
    page.getByLabel("Enable notification preferences"),
  ).not.toBeChecked();
});

test("student route and server-side faculty access restrictions", async ({
  page,
}) => {
  await page.goto("/faculty/dashboard");
  await expect(page).toHaveURL(/login\/faculty/);
  await page.context().addCookies(studentState.cookies);
  await page.goto("/faculty/dashboard");
  await expect(page).toHaveURL(/student\/dashboard/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Welcome Back",
  );
  const res = await page.request.get("/api/v1/faculty/students");
  expect(res.status()).toBe(403);
  const noCsrf = await page.request.post("/api/v1/student/attendance/claim", {
    data: { token: "synthetic-invalid-token" },
  });
  expect(noCsrf.status()).toBe(403);
});

test("faculty logout invalidates the session and protects routes", async ({
  page,
}) => {
  await uiLogin(page);
  await page.getByRole("button", { name: "Open profile menu" }).click();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  expect((await page.request.get("/api/v1/auth/me")).status()).toBe(401);
  await page.goto("/faculty/dashboard");
  await expect(page).toHaveURL(/login\/faculty/);
});

test("dashboard loading state, clear API failure and successful retry", async ({
  page,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "503" });
  await page.context().addCookies(facultyState.cookies);
  let release!: () => void;
  const gate = new Promise<void>((r) => (release = r));
  await page.route("**/api/v1/faculty/dashboard", async (route) => {
    await gate;
    await route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ message: "QA temporary service outage" }),
    });
  });
  await page.goto("/faculty/dashboard");
  await expect(page.getByRole("status")).toContainText(
    "Loading your faculty workspace",
  );
  release();
  await expect(page.getByRole("alert")).toContainText(
    "QA temporary service outage",
  );
  await page.unroute("**/api/v1/faculty/dashboard");
  await page.getByRole("button", { name: "Retry loading" }).click();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.locator(".stat-card strong").first()).toHaveText("03");
});

test("password validation, password change and reauthentication", async ({
  page,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "400" });
  await uiLogin(page);
  await nav(page, "Settings");
  const next = randomBytes(24).toString("base64url");
  await page
    .getByLabel("Current password", { exact: true })
    .fill(randomBytes(16).toString("hex"));
  await page.getByLabel("New password", { exact: true }).fill(next);
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Current password is incorrect",
  );
  await page
    .getByLabel("Current password", { exact: true })
    .fill(env.QA_FACULTY_PASSWORD!);
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(page).toHaveURL(/login\/faculty/);
  await page
    .getByLabel("Employee ID or Official Email", { exact: true })
    .fill(env.QA_FACULTY_ID!);
  await page.getByLabel("Password", { exact: true }).fill(next);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/faculty\/dashboard/);
  const csrf = await (await page.request.get("/api/v1/auth/csrf")).json();
  const restored = await page.request.put("/api/v1/faculty/settings/password", {
    headers: { [csrf.headerName]: csrf.token },
    data: { currentPassword: next, newPassword: env.QA_FACULTY_PASSWORD },
  });
  expect(restored.status()).toBe(200);
});
