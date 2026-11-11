import { startQA, browserPath, root } from "./qa-runtime.mjs";
import { chromium } from "../frontend/node_modules/playwright/index.mjs";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
process.env.PLAYWRIGHT_BROWSERS_PATH = browserPath;
const runtime = await startQA("initial-inspection");
let browser;
const findings = [];
try {
  browser = await chromium.launch({ headless: true, channel: "chromium" });
  console.log("Chromium", browser.version());
  for (const [name, width, height] of [
    ["desktop", 1920, 1080],
    ["laptop", 1366, 768],
    ["tablet", 768, 1024],
    ["mobile", 390, 844],
  ]) {
    const context = await browser.newContext({
      viewport: { width, height },
      timezoneId: "Asia/Kolkata",
    });
    const page = await context.newPage();
    const errors = [];
    const responses = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("console", (m) => {
      if (m.type() === "error") errors.push(m.text());
    });
    page.on("response", (r) => {
      if (r.status() >= 400)
        responses.push({ url: new URL(r.url()).pathname, status: r.status() });
    });
    await page.goto("http://127.0.0.1:5173/");
    await page.getByRole("link", { name: /Faculty Portal/ }).waitFor();
    await page.screenshot({
      path: join(root, "qa/screenshots", name + "-initial-role.png"),
      fullPage: true,
    });
    await page.goto(runtime.env.QA_BASE_URL + "/login/faculty");
    await page.getByRole("button", { name: "Sign In", exact: true }).waitFor();
    await page.screenshot({
      path: join(root, "qa/screenshots", name + "-initial-login.png"),
      fullPage: true,
    });
    const loginWidth = await page.evaluate(() => ({
      viewport: innerWidth,
      document: document.documentElement.scrollWidth,
    }));
    await page
      .getByLabel("Employee ID or Official Email", { exact: true })
      .fill(runtime.env.QA_FACULTY_ID);
    await page
      .getByLabel("Password", { exact: true })
      .fill(runtime.env.QA_FACULTY_PASSWORD);
    await page.getByRole("button", { name: "Sign In", exact: true }).click();
    await page.getByRole("heading", { name: "Quick Access" }).waitFor();
    await page.waitForLoadState("networkidle");
    await page.screenshot({
      path: join(root, "qa/screenshots", name + "-initial-dashboard.png"),
      fullPage: true,
    });
    const dashboardWidth = await page.evaluate(() => ({
      viewport: innerWidth,
      document: document.documentElement.scrollWidth,
      overflow: [...document.querySelectorAll("body *")]
        .filter((e) => {
          const r = e.getBoundingClientRect();
          return r.width > 0 && r.right > innerWidth + 2;
        })
        .map((e) => ({
          tag: e.tagName,
          class: e.className,
          right: Math.round(e.getBoundingClientRect().right),
        }))
        .slice(0, 20),
    }));
    findings.push({ name, loginWidth, dashboardWidth, errors, responses });
    await context.close();
  }
  writeFileSync(
    join(root, "qa/initial-inspection.json"),
    JSON.stringify(findings, null, 2),
  );
  console.log(JSON.stringify(findings, null, 2));
} finally {
  await browser?.close();
  await runtime.stop();
}
