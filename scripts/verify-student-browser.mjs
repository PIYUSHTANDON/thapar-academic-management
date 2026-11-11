import { startQA, browserPath, root } from "./qa-runtime.mjs";
import { join } from "node:path";
import { writeFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
export async function verifyStudentBrowser() {
  process.env.PLAYWRIGHT_BROWSERS_PATH = browserPath;
  const { chromium } =
    await import("../frontend/node_modules/playwright/index.mjs");
  const runtime = await startQA("student-form");
  let browser;
  const results = [];
  try {
    browser = await chromium.launch({ channel: "chromium" });
    for (const [project, width, height] of [
      ["desktop", 1920, 1080],
      ["laptop", 1366, 768],
      ["tablet", 768, 1024],
      ["mobile", 390, 844],
    ]) {
      const context = await browser.newContext({
        viewport: { width, height },
        isMobile: project === "mobile",
        hasTouch: project === "mobile",
        timezoneId: "Asia/Kolkata",
      });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      page.on("console", (m) => {
        if (m.type() === "error") errors.push(m.text());
      });
      page.on("response", (r) => {
        if (r.status() >= 400)
          errors.push(new URL(r.url()).pathname + ": " + r.status());
      });
      await page.goto(runtime.env.QA_BASE_URL + "/login/student");
      await page
        .getByLabel("Enrollment Number or Official Email", { exact: true })
        .fill(runtime.env.QA_STUDENT_ID);
      await page
        .getByLabel("Password", { exact: true })
        .fill(runtime.env.QA_STUDENT_PASSWORD);
      await page.getByRole("button", { name: "Sign In", exact: true }).click();
      await page.waitForURL("**/student/dashboard");
      await page.getByRole("heading", { name: /Welcome Back/ }).waitFor();
      const user = await (
        await page.request.get(runtime.env.QA_BASE_URL + "/api/v1/auth/me")
      ).json();
      if (user.role !== "STUDENT")
        throw Error("Student form authenticated an incorrect role");
      await page.getByRole("button", { name: "Open profile menu" }).click();
      await page.getByRole("button", { name: "Sign out", exact: true }).click();
      await page.waitForURL(
        (url) => url.pathname === "/" || url.pathname === "/login/student",
      );
      await page.waitForLoadState("networkidle");
      if (errors.length)
        throw Error("Student browser errors: " + errors.join("; "));
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth - innerWidth,
      );
      if (overflow > 1) throw Error("Student login overflow");
      results.push({
        project,
        passed: true,
        errors,
        studentLogin: true,
        studentLogout: true,
      });
      await context.close();
      console.log("PASS: student UI login and logout (" + project + ")");
    }
    writeFileSync(
      join(root, "qa/results/student-ui.json"),
      JSON.stringify(results, null, 2),
    );
    return results;
  } finally {
    await browser?.close();
    await runtime.stop();
  }
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href)
  await verifyStudentBrowser();
