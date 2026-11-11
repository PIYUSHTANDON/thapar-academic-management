import { defineConfig } from "@playwright/test";
const project =
  (process.env.QA_PROJECT || "all") +
  (process.env.QA_REPORT_TAG ? "-" + process.env.QA_REPORT_TAG : "");
export default defineConfig({
  testDir: "./e2e",
  testMatch:
    process.env.QA_SUITE === "student" ? "student.spec.ts" : "academic.spec.ts",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60000,
  expect: { timeout: 10000 },
  outputDir: `test-results/${project}`,
  reporter: [
    ["list"],
    ["json", { outputFile: `../qa/results/${project}.json` }],
    ["html", { outputFolder: `../qa/html/${project}`, open: "never" }],
  ],
  use: {
    baseURL: process.env.QA_BASE_URL || "http://127.0.0.1:5174",
    browserName: "chromium",
    channel: "chromium",
    headless: true,
    timezoneId: "Asia/Kolkata",
    locale: "en-IN",
    trace: "off",
    video: "off",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "desktop", use: { viewport: { width: 1920, height: 1080 } } },
    { name: "laptop", use: { viewport: { width: 1366, height: 768 } } },
    { name: "tablet", use: { viewport: { width: 768, height: 1024 } } },
    {
      name: "mobile",
      use: {
        viewport: { width: 390, height: 844 },
        isMobile: true,
        hasTouch: true,
      },
    },
  ],
});
