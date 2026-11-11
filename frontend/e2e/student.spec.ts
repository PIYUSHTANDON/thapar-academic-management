import {
  test as base,
  expect,
  type Page,
  type APIRequestContext,
} from "@playwright/test";
import { randomBytes } from "node:crypto";
import { resolve } from "node:path";
const env = process.env;
let faculty: APIRequestContext,
  student: APIRequestContext,
  facultyState: any,
  studentState: any,
  courses: any[],
  studentId: number;
async function login(api: APIRequestContext, role: "FACULTY" | "STUDENT") {
  const csrf = await (await api.get("/api/v1/auth/csrf")).json();
  const r = await api.post("/api/v1/auth/login", {
    headers: { [csrf.headerName]: csrf.token },
    data: {
      identifier: env[role === "STUDENT" ? "QA_STUDENT_ID" : "QA_FACULTY_ID"],
      password:
        env[role === "STUDENT" ? "QA_STUDENT_PASSWORD" : "QA_FACULTY_PASSWORD"],
      role,
    },
  });
  expect(r.status()).toBe(200);
  return r.json();
}
async function mutate(
  api: APIRequestContext,
  path: string,
  data: any,
  method = "POST",
) {
  const csrf = await (await api.get("/api/v1/auth/csrf")).json();
  return api.fetch("/api/v1" + path, {
    method,
    headers: { [csrf.headerName]: csrf.token },
    data,
  });
}
const test = base.extend<{ studentPage: Page; teacherPage: Page }>({
  page: async ({ page }, use, info) => {
    const errors: string[] = [],
      http: any[] = [],
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
        failed.push(r.failure()?.errorText || "Network request failed");
    });
    await use(page);
    const allowed = info.annotations
      .filter((a) => a.type === "expected-http")
      .map((a) => Number(a.description));
    await info.attach("student-network-audit", {
      body: JSON.stringify({ errors, http, failed }),
      contentType: "application/json",
    });
    expect(http.filter((r) => !allowed.includes(r.status))).toEqual([]);
    expect(
      errors.filter(
        (e) => !allowed.some((code) => e.includes("status of " + code)),
      ),
    ).toEqual([]);
    expect(failed).toEqual([]);
  },
  studentPage: async ({ page }, use) => {
    await page.context().addCookies(studentState.cookies);
    await page.goto("/student/dashboard");
    await expect(
      page.getByRole("heading", { name: "Quick Actions" }),
    ).toBeVisible();
    await expect(page.locator(".loading")).toHaveCount(0);
    await use(page);
  },
  teacherPage: async ({ browser, baseURL }, use, info) => {
    const context = await browser.newContext({
      baseURL,
      storageState: facultyState,
      viewport: info.project.use.viewport,
      timezoneId: "Asia/Kolkata",
    });
    const page = await context.newPage();
    await page.goto("/faculty/dashboard");
    await expect(
      page.getByRole("heading", { name: "Quick Access" }),
    ).toBeVisible();
    await use(page);
    await context.close();
  },
});
async function nav(page: Page, name: string, role = "Student") {
  const open = page.getByRole("button", {
    name: "Open navigation",
    exact: true,
  });
  if (await open.isVisible()) await open.click();
  const link = page
    .getByRole("navigation", { name: role + " navigation" })
    .getByRole("link", { name, exact: true });
  const path = await link.getAttribute("href");
  const changed = new URL(page.url()).pathname !== path;
  const endpoint = path?.endsWith("/dashboard")
    ? "/api/v1/" + role.toLowerCase() + "/dashboard"
    : "/api/v1/" + role.toLowerCase() + "/courses";
  const response = changed
    ? page.waitForResponse(
        (r) => new URL(r.url()).pathname === endpoint && r.ok(),
      )
    : Promise.resolve();
  await link.click();
  await response;
  await expect(page.locator(".loading")).toHaveCount(0);
  await expect(page.locator(".app-shell")).not.toHaveClass(/mobile-open/);
  if (await open.isVisible())
    await expect
      .poll(() =>
        page
          .locator(".sidebar")
          .evaluate((e) => e.getBoundingClientRect().right),
      )
      .toBeLessThanOrEqual(1);
}
async function noOverflow(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth,
    ),
  ).toBeLessThanOrEqual(1);
}
async function uiLogin(page: Page, password = env.QA_STUDENT_PASSWORD!) {
  await page.goto("/login/student");
  await page
    .getByLabel("Enrollment Number or Official Email", { exact: true })
    .fill(env.QA_STUDENT_ID!);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await expect(page).toHaveURL(/student\/dashboard/);
  await expect(page.locator(".loading")).toHaveCount(0);
}
async function shot(page: Page, name: string, project: string) {
  await page.screenshot({
    path: resolve(`../qa/screenshots/student-${project}-${name}.png`),
    fullPage: true,
    animations: "disabled",
  });
}
test.beforeAll(async ({ playwright, baseURL }) => {
  faculty = await playwright.request.newContext({ baseURL });
  student = await playwright.request.newContext({ baseURL });
  await login(faculty, "FACULTY");
  studentId = (await login(student, "STUDENT")).id;
  facultyState = await faculty.storageState();
  studentState = await student.storageState();
  courses = await (await faculty.get("/api/v1/faculty/courses")).json();
  for (const [i, c] of courses.entries()) {
    const r = await mutate(
      faculty,
      "/faculty/courses/" + c.id + "/announcements",
      {
        title: [
          "Demo: course learning plan",
          "Demo: laboratory preparation",
          "Demo: faculty office hours",
        ][i],
        description:
          "Synthetic local announcement. Review your enrolled course materials and assessment deadlines.",
      },
    );
    expect(r.ok()).toBe(true);
  }
});
test.afterAll(async () => {
  await faculty?.dispose();
  await student?.dispose();
});

test("student login, approved dashboard, official CGPA fallback and exactly three quick actions", async ({
  page,
}, info) => {
  await uiLogin(page);
  const d = await (await page.request.get("/api/v1/student/dashboard")).json();
  await expect(page.locator(".student-quick-actions button")).toHaveText([
    "View Timetable",
    "Submit Assignment",
    "Check Results",
  ]);
  await expect(page.locator(".stat-card strong").nth(1)).toHaveText(
    d.currentCgpa === null ? "Not available" : String(d.currentCgpa),
  );
  await expect(page.locator(".stat-card strong").nth(3)).toHaveText(
    String(d.registeredCourses),
  );
  await expect(
    page.getByRole("heading", { name: "Recent Announcements" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Upcoming Deadlines" }),
  ).toBeVisible();
  await expect(page.locator(".student-profile-card")).toContainText(
    "SYNTH-001",
  );
  await expect(page.locator(".student-profile-card")).toContainText(
    "Synthetic local demo record",
  );
  await noOverflow(page);
  await shot(page, "dashboard", info.project.name);
});

test("all eight student routes, mobile drawer and responsive module layouts", async ({
  studentPage: page,
}, info) => {
  for (const [label, path] of [
    ["Dashboard", "dashboard"],
    ["Messages", "messages"],
    ["Attendance", "attendance"],
    ["Courses & LMS", "courses"],
    ["Assessments", "assessments"],
    ["Calendar", "calendar"],
    ["Results", "results"],
    ["Settings", "settings"],
  ]) {
    await nav(page, label);
    await expect(page).toHaveURL(new RegExp("/student/" + path + "$"));
    await noOverflow(page);
    await shot(page, path + "-module", info.project.name);
  }
  if (
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .isVisible()
  ) {
    await page
      .getByRole("button", { name: "Open navigation", exact: true })
      .click();
    await page.keyboard.press("Escape");
    await expect(page.locator(".app-shell")).not.toHaveClass(/mobile-open/);
  } else {
    await page.getByRole("button", { name: "Toggle sidebar" }).click();
    await expect(page.locator(".app-shell")).toHaveClass(/sidebar-collapsed/);
    await page.getByRole("button", { name: "Toggle sidebar" }).click();
  }
});

test("quick-action destinations, course search, notification read and announcement details", async ({
  studentPage: page,
}) => {
  for (const [label, path] of [
    ["View Timetable", "calendar"],
    ["Submit Assignment", "assessments"],
    ["Check Results", "results"],
  ]) {
    await page.getByRole("button", { name: label, exact: true }).click();
    await expect(page).toHaveURL(new RegExp("/student/" + path + "$"));
    await expect(page.locator(".loading")).toHaveCount(0);
    await nav(page, "Dashboard");
  }
  if (
    await page
      .getByRole("button", { name: "Open search", exact: true })
      .isVisible()
  )
    await page
      .getByRole("button", { name: "Open search", exact: true })
      .click();
  await page
    .getByRole("textbox", { name: "Search courses and resources" })
    .fill("Data");
  await expect(page.locator(".search-results a")).not.toHaveCount(0);
  await page.locator(".search-results a").first().click();
  await expect(page).toHaveURL(/student\/courses/);
  await nav(page, "Dashboard");
  await page
    .getByRole("button", { name: "Notifications", exact: true })
    .click();
  await page.locator(".notifications>button").first().click();
  await expect(page.locator(".notifications>button").first()).toContainText(
    "Read",
  );
  await page.keyboard.press("Escape");
  await page.locator(".student-announcements>button").first().click();
  await expect(page.getByRole("dialog")).toContainText(
    "Synthetic local announcement",
  );
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
});

test("faculty assignment to student upload to published feedback and results", async ({
  studentPage: page,
  teacherPage: teacher,
}) => {
  const title = "QA synchronized assignment " + randomBytes(3).toString("hex");
  await nav(teacher, "Assessments", "Faculty");
  await teacher.getByLabel("Title", { exact: true }).fill(title);
  await teacher
    .getByRole("combobox", { name: "Type", exact: true })
    .selectOption("Assignment");
  await teacher
    .getByLabel("Deadline", { exact: true })
    .fill("2099-12-01T12:00");
  await teacher
    .getByRole("button", { name: "Create assessment", exact: true })
    .click();
  await expect(teacher.locator(".toast")).toContainText("Assessment created");
  await nav(page, "Assessments");
  let panel = page
    .locator(".student-assessments>section")
    .filter({ hasText: title });
  await expect(panel).toBeVisible();
  await panel
    .getByRole("textbox", { name: "Submission text", exact: true })
    .fill("Synthetic synchronized submission");
  await panel.getByLabel("Assignment file", { exact: true }).setInputFiles({
    name: "student-work.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("Synthetic student attachment"),
  });
  await panel
    .getByRole("button", { name: "Submit assignment", exact: true })
    .click();
  await expect(page.locator(".toast")).toContainText("Assignment submitted");
  await page.reload();
  await expect(panel).toContainText("student-work.txt");
  await teacher.reload();
  const sub = teacher.locator(".submission").filter({ hasText: title });
  const [download] = await Promise.all([
    teacher.waitForEvent("download"),
    sub.getByRole("link", { name: "Download student-work.txt" }).click(),
  ]);
  expect(await download.failure()).toBeNull();
  await sub.getByLabel("Marks / 100").fill("87");
  await sub
    .getByRole("textbox", { name: "Faculty feedback", exact: true })
    .fill("Thoughtful work with clear reasoning.");
  await sub.getByRole("button", { name: "Save grade", exact: true }).click();
  await expect(teacher.locator(".toast")).toContainText("Grade saved");
  await nav(page, "Results");
  await expect(page.locator("tbody")).not.toContainText(title);
  const assessment = teacher
    .locator(".assessment-item")
    .filter({ hasText: title });
  await assessment
    .getByRole("button", { name: "Publish results", exact: true })
    .click();
  await expect(teacher.locator(".toast")).toContainText("Results published");
  await page.reload();
  const row = page.locator("tbody tr").filter({ hasText: title });
  await expect(row).toContainText("87");
  await expect(row).toContainText("Thoughtful work with clear reasoning.");
  expect(
    (await (await student.get("/api/v1/student/dashboard")).json()).currentCgpa,
  ).toBeNull();
});

test("student and faculty messages synchronize in both directions", async ({
  studentPage: page,
  teacherPage: teacher,
}) => {
  const text = "Student question " + randomBytes(3).toString("hex"),
    reply = "Faculty reply " + randomBytes(3).toString("hex");
  await nav(page, "Messages");
  await page.getByRole("textbox", { name: "Message", exact: true }).fill(text);
  await page.getByRole("button", { name: "Send message" }).click();
  await expect(page.locator(".toast")).toContainText("Message sent");
  await nav(teacher, "Messages", "Faculty");
  await expect(teacher.locator(".message-history")).toContainText(text);
  await teacher
    .getByRole("textbox", { name: "Message", exact: true })
    .fill(reply);
  await teacher.getByRole("button", { name: "Send message" }).click();
  await expect(teacher.locator(".toast")).toContainText("Message sent");
  await page.reload();
  await expect(page.locator(".message-history")).toContainText(reply);
});

test("faculty material and announcements appear in student LMS after refresh", async ({
  studentPage: page,
  teacherPage: teacher,
}) => {
  await nav(teacher, "Courses & LMS", "Faculty");
  await teacher.getByLabel("Learning resource file").setInputFiles({
    name: "shared-notes.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("Synthetic shared course notes"),
  });
  await teacher.getByRole("button", { name: "Upload", exact: true }).click();
  await expect(teacher.locator(".toast")).toContainText("Resource uploaded");
  const title = "Shared notice " + randomBytes(3).toString("hex");
  await teacher.getByLabel("Announcement", { exact: true }).fill(title);
  await teacher
    .getByRole("textbox", { name: "Announcement description", exact: true })
    .fill("Complete announcement details from the course faculty.");
  await teacher.getByRole("button", { name: "Publish announcement" }).click();
  await expect(teacher.locator(".toast")).toContainText("Announcement shared");
  await nav(page, "Courses & LMS");
  await expect(page.locator(".resource-row")).toContainText([
    "shared-notes.txt",
    title,
  ]);
  await noOverflow(page);
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("link", { name: "Download shared-notes.txt" }).click(),
  ]);
  expect(await download.failure()).toBeNull();
  await page
    .locator(".resource-row")
    .filter({ hasText: title })
    .getByRole("button", { name: "Read announcement" })
    .click();
  await expect(page.getByRole("dialog")).toContainText(
    "Complete announcement details",
  );
  await page.getByRole("button", { name: "Close announcement" }).click();
});

test("faculty QR image decodes locally, student claims attendance and replay is rejected", async ({
  studentPage: page,
  teacherPage: teacher,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "409" });
  await nav(teacher, "Attendance", "Faculty");
  await teacher.getByLabel("Class session").selectOption({ index: 0 });
  await teacher.getByRole("button", { name: "Generate attendance QR" }).click();
  const image = teacher.getByRole("img", {
    name: "Short-lived attendance QR for authenticated enrolled students",
  });
  await expect(image).toBeVisible();
  const src = await image.getAttribute("src");
  await nav(page, "Attendance");
  await page.getByLabel("Attendance QR image").setInputFiles({
    name: "attendance.png",
    mimeType: "image/png",
    buffer: Buffer.from(src!.split(",")[1], "base64"),
  });
  await expect(page.locator(".toast")).toContainText("Attendance QR decoded");
  const token = await page
    .getByRole("textbox", { name: "Attendance token" })
    .inputValue();
  await page.getByRole("button", { name: "Claim attendance" }).click();
  await expect(page.locator(".toast")).toContainText("Attendance recorded");
  await teacher.reload();
  await expect(teacher.locator("tbody")).toContainText("Demo Student");
  await page.getByRole("textbox", { name: "Attendance token" }).fill(token);
  await page.getByRole("button", { name: "Claim attendance" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Attendance already recorded",
  );
  await page.getByRole("textbox", { name: "Attendance token" }).clear();
});

test("calendar events, month navigation and faculty timetable edits synchronize", async ({
  studentPage: page,
  teacherPage: teacher,
}) => {
  await nav(page, "Calendar");
  const month = await page.locator(".month-heading h2").textContent();
  await page.getByRole("button", { name: "Next month" }).click();
  await expect(page.locator(".month-heading h2")).not.toHaveText(month!);
  await page.getByRole("button", { name: "Previous month" }).click();
  await expect(page.locator(".month-heading h2")).toHaveText(month!);
  await nav(teacher, "Calendar", "Faculty");
  const title = "Shared academic event " + randomBytes(3).toString("hex"),
    date = new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
    }).format(new Date());
  await teacher.getByLabel("Event title").fill(title);
  await teacher.getByLabel("Date", { exact: true }).fill(date);
  await teacher.getByRole("button", { name: "Add event", exact: true }).click();
  await expect(teacher.locator(".toast")).toContainText("Event added");
  await page.reload();
  await page.getByRole("button", { name: date, exact: true }).click();
  await expect(page.locator(".calendar-events")).toContainText(title);
  await teacher
    .getByRole("combobox", { name: "Teaching session", exact: true })
    .selectOption({ index: 1 });
  await teacher.getByLabel("Class room").fill("QA-LT-303");
  await teacher.getByRole("button", { name: "Save class information" }).click();
  await expect(
    teacher.getByText("Class information updated", { exact: true }),
  ).toBeVisible();
  expect(
    JSON.stringify(
      await (await student.get("/api/v1/student/schedule")).json(),
    ),
  ).toContain("QA-LT-303");
  await page.reload();
  await expect(page.locator(".calendar-events")).toContainText("QA-LT-303");
});

test("role restrictions, upload errors and protected official academic fields", async ({
  studentPage: page,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "400" });
  expect((await student.get("/api/v1/faculty/students")).status()).toBe(403);
  expect((await faculty.get("/api/v1/student/dashboard")).status()).toBe(403);
  expect(
    (await student.get("/api/v1/student/courses/999999/resources")).status(),
  ).toBe(403);
  const r = await mutate(faculty, "/faculty/assessments", {
    courseId: courses[0].id,
    title: "Upload validation assessment",
    kind: "Assignment",
    deadline: "2099-12-01T12:00:00",
  });
  expect(r.ok()).toBe(true);
  await nav(page, "Assessments");
  const panel = page
    .locator(".student-assessments>section")
    .filter({ hasText: "Upload validation assessment" });
  await panel.getByLabel("Assignment file", { exact: true }).setInputFiles({
    name: "unsupported.exe",
    mimeType: "application/octet-stream",
    buffer: Buffer.from("MZ"),
  });
  await panel
    .getByRole("button", { name: "Submit assignment", exact: true })
    .click();
  await expect(page.getByRole("alert")).toContainText("Supported files");
});

test("settings preferences persist and student logout protects the dashboard", async ({
  page,
}) => {
  await uiLogin(page);
  await nav(page, "Settings");
  await page.getByLabel("Enable notification preferences").uncheck();
  await page.getByRole("button", { name: "Save changes", exact: true }).click();
  await expect(page.locator(".toast")).toContainText("Profile updated");
  await page.reload();
  await expect(
    page.getByLabel("Enable notification preferences"),
  ).not.toBeChecked();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page).toHaveURL(/login\/student|\/$/);
  await page.goto("/student/dashboard");
  await expect(page).toHaveURL(/login\/student/);
});

test("student password change refreshes CSRF and accepts only the new password", async ({
  page,
}, info) => {
  info.annotations.push({ type: "expected-http", description: "400" });
  await uiLogin(page);
  await nav(page, "Settings");
  const next = randomBytes(24).toString("base64url");
  await page
    .getByLabel("Current password", { exact: true })
    .fill(randomBytes(20).toString("hex"));
  await page.getByLabel("New password", { exact: true }).fill(next);
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Current password is incorrect",
  );
  await page
    .getByLabel("Current password", { exact: true })
    .fill(env.QA_STUDENT_PASSWORD!);
  await page.getByRole("button", { name: "Update password" }).click();
  await expect(page).toHaveURL(/login\/student/);
  await uiLogin(page, next);
  const csrf = await (await page.request.get("/api/v1/auth/csrf")).json();
  expect(
    (
      await page.request.put("/api/v1/student/settings/password", {
        headers: { [csrf.headerName]: csrf.token },
        data: { currentPassword: next, newPassword: env.QA_STUDENT_PASSWORD },
      })
    ).status(),
  ).toBe(200);
});
