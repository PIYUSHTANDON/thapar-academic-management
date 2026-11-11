import { join } from "node:path";
import { writeFileSync, mkdirSync } from "node:fs";
const password = process.env.LOCAL_DEMO_PASSWORD;
const identifier = process.env.LOCAL_DEMO_ID || "FACULTY001";
if (!password)
  throw Error(
    "Supply LOCAL_DEMO_PASSWORD in the process environment; no password is stored in this script.",
  );
process.env.PLAYWRIGHT_BROWSERS_PATH ||= join(
  process.cwd(),
  ".tools/playwright",
);
const { chromium, request } =
  await import("../frontend/node_modules/playwright/index.mjs");
const baseURL = "http://127.0.0.1:5173";
const api = await request.newContext({ baseURL });
let browser;
try {
  let csrf = await (await api.get("/api/v1/auth/csrf")).json();
  const auth = await api.post("/api/v1/auth/login", {
    headers: { [csrf.headerName]: csrf.token },
    data: { identifier, password, role: "FACULTY" },
  });
  if (auth.status() !== 200)
    throw Error("Backend authentication failed: " + auth.status());
  const user = await auth.json();
  if (
    user.identifier !== identifier ||
    user.name !== "Demo Faculty" ||
    user.role !== "FACULTY"
  )
    throw Error("Account identity or role conflict");
  const dashboard = await (await api.get("/api/v1/faculty/dashboard")).json();
  if (dashboard.activeClasses < 1) throw Error("No demo courses associated");
  csrf = await (await api.get("/api/v1/auth/csrf")).json();
  await api.post("/api/v1/auth/logout", {
    headers: { [csrf.headerName]: csrf.token },
  });
  browser = await chromium.launch({ channel: "chromium" });
  const page = await browser.newPage({
    baseURL,
    viewport: { width: 1366, height: 768 },
    timezoneId: "Asia/Kolkata",
  });
  const errors = [],
    failed = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", (m) => {
    if (m.type() === "error") errors.push(m.text());
  });
  page.on("response", (r) => {
    if (r.status() >= 400)
      failed.push({ path: new URL(r.url()).pathname, status: r.status() });
  });
  await page.goto(baseURL + "/login/faculty");
  await page
    .getByLabel("Employee ID or Official Email", { exact: true })
    .fill(identifier);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign In", exact: true }).click();
  await page.waitForURL("**/faculty/dashboard");
  await page.getByRole("heading", { name: "Quick Access" }).waitFor();
  await page.waitForLoadState("networkidle");
  const actual = await (await page.request.get("/api/v1/auth/me")).json();
  if (actual.name !== "Demo Faculty" || actual.role !== "FACULTY")
    throw Error("Browser authenticated an incorrect user");
  const counts = {};
  for (const path of ["courses", "assessments", "attendance", "schedule"]) {
    const r = await page.request.get("/api/v1/faculty/" + path);
    if (!r.ok()) throw Error("Faculty API failed: " + path);
    counts[path] = (await r.json()).length;
  }
  if (
    counts.courses < 1 ||
    counts.assessments < 1 ||
    counts.attendance < 1 ||
    counts.schedule < 1
  )
    throw Error("Synthetic teaching data missing");
  if (errors.length || failed.length) throw Error("Unexpected browser errors");
  mkdirSync("qa/screenshots", { recursive: true });
  await page.screenshot({
    path: "qa/screenshots/local-faculty-demo-dashboard.png",
    fullPage: true,
    animations: "disabled",
  });
  const result = {
    url: baseURL + "/login/faculty",
    identifier,
    name: actual.name,
    role: actual.role,
    backendAuthentication: true,
    browserAuthentication: true,
    dashboardLoaded: true,
    statistics: dashboard,
    associatedRecords: counts,
    errors,
    failed,
  };
  mkdirSync("qa/results", { recursive: true });
  writeFileSync(
    "qa/results/local-faculty-demo.json",
    JSON.stringify(result, null, 2),
  );
  console.log(
    JSON.stringify(
      {
        url: result.url,
        identifier,
        name: actual.name,
        backendAuthentication: true,
        browserAuthentication: true,
        dashboardLoaded: true,
        associatedRecords: counts,
        errors,
        failed,
      },
      null,
      2,
    ),
  );
} finally {
  await api.dispose();
  await browser?.close();
}
