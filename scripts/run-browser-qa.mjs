import { startQA, root, browserPath } from "./qa-runtime.mjs";
import { spawn } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
process.env.PLAYWRIGHT_BROWSERS_PATH = browserPath;
const selected = process.env.QA_PROJECT;
const projects = selected
  ? [selected]
  : ["desktop", "laptop", "tablet", "mobile"];
const results = [];
for (const project of projects) {
  if (!["desktop", "laptop", "tablet", "mobile"].includes(project))
    throw Error("Unknown QA_PROJECT");
  const runtime = await startQA(project);
  let passed = false;
  let persistence = false;
  try {
    await runtime.prepareAttendance();
    console.log(
      `Running ${project} against ${runtime.env.QA_BASE_URL}; Java: ${runtime.java}`,
    );
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
        env: { ...runtime.env, QA_PROJECT: project },
        windowsHide: true,
        stdio: "inherit",
      },
    );
    const code = await new Promise((resolve, reject) => {
      child.on("error", reject);
      child.on("exit", resolve);
    });
    passed = code === 0;
    if (passed) {
      const { request } =
        await import("../frontend/node_modules/playwright/index.mjs");
      const auth = async () => {
        const client = await request.newContext({
          baseURL: runtime.env.QA_BASE_URL,
        });
        let csrf = await (await client.get("/api/v1/auth/csrf")).json();
        const response = await client.post("/api/v1/auth/login", {
          headers: { [csrf.headerName]: csrf.token },
          data: {
            identifier: runtime.env.QA_FACULTY_ID,
            password: runtime.env.QA_FACULTY_PASSWORD,
            role: "FACULTY",
          },
        });
        if (!response.ok()) throw Error("Persistence check login failed");
        csrf = await (await client.get("/api/v1/auth/csrf")).json();
        return { client, csrf };
      };
      let { client, csrf } = await auth();
      const title = "Restart persistence canary " + project;
      const response = await client.post("/api/v1/faculty/calendar", {
        headers: { [csrf.headerName]: csrf.token },
        data: { title, eventDate: "2099-01-01" },
      });
      if (!response.ok()) throw Error("Persistence canary creation failed");
      await client.dispose();
      await runtime.restartBackend();
      ({ client } = await auth());
      const events = await (
        await client.get("/api/v1/faculty/calendar")
      ).json();
      persistence = events.some((e) => e.title === title);
      await client.dispose();
      if (!persistence) throw Error("Calendar event lost after server restart");
      console.log(
        "PASS: database persistence after Java restart (" + project + ")",
      );
    }
  } catch (error) {
    console.error(error.message);
    passed = false;
  } finally {
    await runtime.stop();
  }
  results.push({ project, passed, persistence, java: runtime.java });
}
if (!selected && results.every((result) => result.passed)) {
  const { verifyStudentBrowser } = await import("./verify-student-browser.mjs");
  await verifyStudentBrowser();
}
mkdirSync(join(root, "qa/results"), { recursive: true });
writeFileSync(
  join(
    root,
    "qa/results/run-summary" +
      (process.env.QA_REPORT_TAG ? "-" + process.env.QA_REPORT_TAG : "") +
      ".json",
  ),
  JSON.stringify(results, null, 2),
);
if (results.some((r) => !r.passed)) process.exitCode = 1;
