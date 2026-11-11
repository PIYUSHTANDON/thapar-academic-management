import { startQA, root, browserPath } from "./qa-runtime.mjs";
import { spawn } from "node:child_process";
import { join } from "node:path";
import { mkdirSync, writeFileSync } from "node:fs";
process.env.PLAYWRIGHT_BROWSERS_PATH = browserPath;
const projects = process.env.QA_PROJECT
  ? [process.env.QA_PROJECT]
  : ["desktop", "laptop", "tablet", "mobile"];
const results = [];
for (const project of projects) {
  const runtime = await startQA("student-" + project);
  let passed = false;
  try {
    await runtime.prepareAttendance();
    const child = spawn(
      process.execPath,
      [
        join(root, "frontend/node_modules/@playwright/test/cli.js"),
        "test",
        "--project",
        project,
        ...process.argv.slice(2),
      ],
      {
        cwd: join(root, "frontend"),
        env: {
          ...runtime.env,
          QA_PROJECT: project,
          QA_SUITE: "student",
          QA_REPORT_TAG: process.env.QA_REPORT_TAG || "student-phase3",
        },
        windowsHide: true,
        stdio: "inherit",
      },
    );
    passed =
      (await new Promise((resolve, reject) => {
        child.on("error", reject);
        child.on("exit", resolve);
      })) === 0;
  } finally {
    await runtime.stop();
  }
  results.push({ project, passed });
}
mkdirSync(join(root, "qa/results"), { recursive: true });
writeFileSync(
  join(root, "qa/results/student-phase3-summary.json"),
  JSON.stringify(results, null, 2),
);
if (results.some((r) => !r.passed)) process.exitCode = 1;
