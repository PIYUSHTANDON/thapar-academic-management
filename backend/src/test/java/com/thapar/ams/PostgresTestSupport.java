package com.thapar.ams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Configures isolated per-test-class schemas inside the local PostgreSQL test database (thapar_ams_test).
 */
public final class PostgresTestSupport {

  private PostgresTestSupport() {}

  public static String register(
    DynamicPropertyRegistry registry,
    String prefix
  ) {
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    Map<String, String> envFile = loadDotEnvPostgres();
    String port = firstNonBlank(
      System.getenv("POSTGRES_PORT"),
      envFile.get("POSTGRES_PORT"),
      "55432"
    );
    String user = firstNonBlank(System.getenv("DB_USER"), "ams");
    String password = firstNonBlank(
      System.getenv("DB_PASSWORD"),
      envFile.get("DB_PASSWORD")
    );
    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
        "PostgreSQL DB_PASSWORD is required for tests (run scripts/start-postgres.ps1 to generate .env.postgres)."
      );
    }

    String cleanPrefix = prefix.replaceAll("[^a-z0-9_]", "");
    String schema =
      "t_" + cleanPrefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String baseUrl = "jdbc:postgresql://127.0.0.1:" + port + "/thapar_ams_test";
    String url = baseUrl + "?currentSchema=" + schema;

    registry.add("spring.datasource.url", () -> url);
    registry.add("spring.datasource.username", () -> user);
    registry.add("spring.datasource.password", () -> password);
    registry.add("spring.flyway.default-schema", () -> schema);
    registry.add("spring.flyway.schemas", () -> schema);
    registry.add("spring.flyway.create-schemas", () -> "true");
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> schema);

    Runtime.getRuntime().addShutdownHook(
      new Thread(
        () -> dropSchemaQuietly(baseUrl, user, password, schema),
        "pg-test-cleanup-" + schema
      )
    );
    return schema;
  }

  private static void dropSchemaQuietly(
    String baseUrl,
    String user,
    String password,
    String schema
  ) {
    try (
      var conn = DriverManager.getConnection(baseUrl, user, password);
      var st = conn.createStatement()
    ) {
      st.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
    } catch (Exception ignored) {}
  }

  private static Map<String, String> loadDotEnvPostgres() {
    Map<String, String> result = new HashMap<>();
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve(".env.postgres");
    if (!Files.isRegularFile(candidate) && cwd.getParent() != null) {
      candidate = cwd.getParent().resolve(".env.postgres");
    }
    if (!Files.isRegularFile(candidate)) return result;
    try {
      for (String line : Files.readAllLines(candidate, StandardCharsets.UTF_8)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int eq = trimmed.indexOf('=');
        if (eq > 0) {
          result.put(
            trimmed.substring(0, eq).trim(),
            trimmed.substring(eq + 1).trim()
          );
        }
      }
    } catch (IOException ignored) {}
    return result;
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) return v.trim();
    }
    return null;
  }
}
