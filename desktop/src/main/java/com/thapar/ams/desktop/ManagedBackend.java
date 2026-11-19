package com.thapar.ams.desktop;

import com.thapar.ams.desktop.api.ApiClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Starts and stops the packaged Spring Boot backend (PostgreSQL-backed) when launching via single-command Maven mode.
 */
final class ManagedBackend implements AutoCloseable {
  private static final int PREFERRED_PORT = 8081;
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

  private final Process process;
  private final URI apiBase;

  private ManagedBackend(Process process, URI apiBase) {
    this.process = process;
    this.apiBase = apiBase;
  }

  static ManagedBackend startIfRequested() throws Exception {
    if (!Boolean.getBoolean("ams.managedBackend") || hasExternalBaseUrl()) return null;

    Path root = resolveProjectRoot(System.getProperty("ams.projectRoot"));
    Path backendJar = root.resolve("backend").resolve("target").resolve("ams-1.0.0.jar");
    if (!Files.isRegularFile(backendJar)) {
      throw new IllegalStateException("Backend JAR not found at " + backendJar);
    }

    Map<String, String> envFile = loadDotEnvPostgres(root);
    String pgPort = firstNonBlank(System.getenv("POSTGRES_PORT"), envFile.get("POSTGRES_PORT"), "55432");
    String dbUrl = firstNonBlank(
      System.getenv("DB_URL"),
      "jdbc:postgresql://127.0.0.1:" + pgPort + "/thapar_ams?currentSchema=academic"
    );
    String dbUser = firstNonBlank(System.getenv("DB_USER"), "ams");
    String dbPassword = firstNonBlank(System.getenv("DB_PASSWORD"), envFile.get("DB_PASSWORD"));
    if (dbPassword == null || dbPassword.isBlank()) {
      throw new IllegalStateException(
        "PostgreSQL DB_PASSWORD is missing. Run scripts/start-postgres.ps1 first to start PostgreSQL and create .env.postgres."
      );
    }
    ensureLocalPostgresReachable(pgPort, dbUrl);

    int port = selectPort(PREFERRED_PORT);
    URI base = URI.create("http://127.0.0.1:" + port + "/api/v1");
    Path logFile = root.resolve(".tools").resolve("app-backend.log");
    Files.createDirectories(logFile.getParent());

    ProcessBuilder builder = new ProcessBuilder(
      resolveJavaExecutable(),
      "-jar",
      backendJar.toString(),
      "--server.port=" + port
    );
    builder.directory(root.toFile());
    var env = builder.environment();
    env.putIfAbsent("SPRING_PROFILES_ACTIVE", "local");
    env.put("DB_URL", dbUrl);
    env.put("DB_USER", dbUser);
    env.put("DB_PASSWORD", dbPassword);
    builder.redirectErrorStream(true);
    builder.redirectOutput(logFile.toFile());

    Process process = builder.start();
    var managed = new ManagedBackend(process, base);
    Runtime.getRuntime().addShutdownHook(new Thread(managed::close, "ams-backend-shutdown"));
    waitUntilReady(managed, logFile);
    System.setProperty("ams.api.base", base.toString());
    return managed;
  }

  URI apiBase() {
    return apiBase;
  }

  @Override
  public void close() {
    if (!process.isAlive()) return;
    process.destroy();
    try {
      if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }

  static boolean hasExternalBaseUrl() {
    String prop = System.getProperty("ams.api.base");
    String env = System.getenv("AMS_API_BASE");
    return (prop != null && !prop.isBlank()) || (env != null && !env.isBlank());
  }

  static int selectPort(int preferredPort) throws IOException {
    if (preferredPort > 0) {
      try (var socket = new ServerSocket(preferredPort)) {
        return socket.getLocalPort();
      } catch (IOException ignored) {
        // Fall back to an ephemeral port if preferredPort is occupied.
      }
    }
    try (var socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  static Map<String, String> loadDotEnvPostgres(Path root) {
    Map<String, String> result = new HashMap<>();
    Path file = root.resolve(".env.postgres");
    if (!Files.isRegularFile(file)) return result;
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int eq = trimmed.indexOf('=');
        if (eq > 0) {
          result.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
        }
      }
    } catch (IOException ignored) {}
    return result;
  }

  private static void ensureLocalPostgresReachable(String pgPort, String dbUrl) {
    if (!dbUrl.startsWith("jdbc:postgresql://127.0.0.1:") && !dbUrl.startsWith("jdbc:postgresql://localhost:")) {
      return;
    }
    int parsedPort;
    try {
      parsedPort = Integer.parseInt(pgPort);
    } catch (NumberFormatException e) {
      return;
    }
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", parsedPort), 1500);
    } catch (IOException e) {
      throw new IllegalStateException(
        "PostgreSQL is not reachable at 127.0.0.1:" + parsedPort
          + ". Start Docker Desktop and run: powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-postgres.ps1",
        e
      );
    }
  }

  private static Path resolveProjectRoot(String configuredRoot) {
    if (configuredRoot != null && !configuredRoot.isBlank()) {
      return Path.of(configuredRoot.trim()).toAbsolutePath().normalize();
    }
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (Files.isRegularFile(cwd.resolve("backend").resolve("target").resolve("ams-1.0.0.jar"))) return cwd;
    Path parent = cwd.getParent();
    if (parent != null && Files.isRegularFile(parent.resolve("backend").resolve("target").resolve("ams-1.0.0.jar"))) {
      return parent;
    }
    return cwd;
  }

  private static String resolveJavaExecutable() {
    boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    Path candidate = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
    return Files.isRegularFile(candidate) ? candidate.toString() : "java";
  }

  private static void waitUntilReady(ManagedBackend managed, Path logFile) throws Exception {
    var probe = new ApiClient(managed.apiBase);
    long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      if (!managed.process.isAlive()) {
        throw new IllegalStateException(
          "Managed backend exited with code " + managed.process.exitValue() + ":\n" + tail(logFile)
        );
      }
      try {
        probe.get("health");
        return;
      } catch (ApiClient.ApiException ignored) {
        Thread.sleep(300);
      }
    }
    managed.close();
    throw new IllegalStateException("Managed backend did not become ready on " + managed.apiBase + "\n" + tail(logFile));
  }

  private static String tail(Path logFile) {
    try {
      if (!Files.isRegularFile(logFile)) return "(no log file)";
      List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
      int from = Math.max(0, lines.size() - 30);
      return String.join(System.lineSeparator(), lines.subList(from, lines.size()));
    } catch (IOException e) {
      return "(failed to read log: " + e.getMessage() + ")";
    }
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) return v.trim();
    }
    return null;
  }
}
