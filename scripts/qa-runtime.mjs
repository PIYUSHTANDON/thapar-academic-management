import { spawn, spawnSync } from "node:child_process";
import { createWriteStream, existsSync, mkdirSync, readdirSync } from "node:fs";
import { resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { randomBytes } from "node:crypto";
export const root = resolve(fileURLToPath(new URL("..", import.meta.url)));
export const browserPath = join(root, ".tools/playwright");
export function javaPath() {
  if (process.env.QA_JAVA_HOME)
    return join(process.env.QA_JAVA_HOME, "bin/java.exe");
  const dir = join(root, ".tools/jdk21");
  if (existsSync(dir)) {
    const found = readdirSync(dir).find((x) =>
      existsSync(join(dir, x, "bin/java.exe")),
    );
    if (found) return join(dir, found, "bin/java.exe");
  }
  const result = spawnSync("java", ["-XshowSettings:properties", "-version"], {
    encoding: "utf8",
    windowsHide: true,
  });
  const home = result.stderr.match(/java.home = (.+)/)?.[1]?.trim();
  if (!home) throw Error("No Java runtime found");
  return join(home, "bin", process.platform === "win32" ? "java.exe" : "java");
}
const pause = (ms) => new Promise((r) => setTimeout(r, ms));
export async function ready(url, child) {
  for (let i = 0; i < 120; i++) {
    if (child?.exitCode !== null && child?.exitCode !== undefined)
      throw Error("QA server exited before ready: " + url);
    try {
      const res = await fetch(url);
      if (res.ok) return;
    } catch {}
    await pause(500);
  }
  throw Error("Readiness timeout: " + url);
}
export async function startQA(name = "inspection") {
  mkdirSync(join(root, ".tools/qa"), { recursive: true });
  const nonce = randomBytes(5).toString("hex");
  const env = {
    ...process.env,
    DEBUG: "false",
    SPRING_PROFILES_ACTIVE: "local",
    SEED_ENABLED: "true",
    SEED_FACULTY_ID: "qa-faculty-" + nonce,
    SEED_FACULTY_PASSWORD: randomBytes(24).toString("base64url"),
    SEED_STUDENT_ID: "qa-student-" + nonce,
    SEED_STUDENT_PASSWORD: randomBytes(24).toString("base64url"),
    SERVER_PORT: "8082",
    SPRING_DATASOURCE_URL:
      "jdbc:h2:file:" +
      join(root, ".tools/qa", name + "-" + nonce).replaceAll("\\", "/") +
      ";MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0",
    VITE_API_PROXY_TARGET: "http://127.0.0.1:8082",
    PLAYWRIGHT_BROWSERS_PATH: browserPath,
  };
  Object.assign(env, {
    QA_FACULTY_ID: env.SEED_FACULTY_ID,
    QA_FACULTY_PASSWORD: env.SEED_FACULTY_PASSWORD,
    QA_STUDENT_ID: env.SEED_STUDENT_ID,
    QA_STUDENT_PASSWORD: env.SEED_STUDENT_PASSWORD,
    QA_BASE_URL: "http://127.0.0.1:5174",
  });
  const log = createWriteStream(join(root, ".tools/qa", name + ".log"), {
    flags: "w",
  });
  let backend;
  const boot = () => {
    backend = spawn(
      javaPath(),
      ["-jar", join(root, "backend/target/ams-1.0.0.jar")],
      { cwd: root, env, windowsHide: true, stdio: ["ignore", "pipe", "pipe"] },
    );
    backend.stdout.pipe(log, { end: false });
    backend.stderr.pipe(log, { end: false });
    return ready("http://127.0.0.1:8082/api/v1/health", backend);
  };
  let vite;
  const stopChild = async (child) => {
    if (child && child.exitCode === null) {
      child.kill();
      await Promise.race([
        new Promise((r) => child.once("exit", r)),
        pause(8000),
      ]);
    }
  };
  try {
    await boot();
    vite = spawn(
      process.execPath,
      [
        join(root, "frontend/node_modules/vite/bin/vite.js"),
        "--host",
        "127.0.0.1",
        "--port",
        "5174",
        "--strictPort",
      ],
      {
        cwd: join(root, "frontend"),
        env,
        windowsHide: true,
        stdio: ["ignore", "pipe", "pipe"],
      },
    );
    vite.stdout.pipe(log, { end: false });
    vite.stderr.pipe(log, { end: false });
    await ready(env.QA_BASE_URL, vite);
    return {
      env,
      root,
      java: javaPath(),
      prepareAttendance: async () => {
        await stopChild(backend);
        const h2 = join(
          root,
          ".tools/m2/com/h2database/h2/2.3.232/h2-2.3.232.jar",
        );
        const fixture = spawnSync(
          javaPath(),
          [
            "-cp",
            h2,
            join(root, "scripts/QaAttendanceFixture.java"),
            env.SPRING_DATASOURCE_URL,
          ],
          { cwd: root, env, windowsHide: true, encoding: "utf8" },
        );
        if (fixture.status !== 0)
          throw Error("Attendance fixture failed: " + fixture.stderr);
        await boot();
      },
      restartBackend: async () => {
        await stopChild(backend);
        await boot();
      },
      stop: async () => {
        await stopChild(vite);
        await stopChild(backend);
        log.end();
      },
    };
  } catch (e) {
    await stopChild(vite);
    await stopChild(backend);
    log.end();
    throw e;
  }
}
