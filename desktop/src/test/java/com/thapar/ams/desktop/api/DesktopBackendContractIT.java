package com.thapar.ams.desktop.api;

import static org.junit.jupiter.api.Assertions.*;

import com.thapar.ams.contract.AuthContracts.UserRole;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopBackendContractIT {
  private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
  private static final String FACULTY_ID = "it-faculty-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String STUDENT_ID = "it-student-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String FACULTY_PASSWORD = "Faculty-" + UUID.randomUUID();
  private static final String STUDENT_PASSWORD = "Student-" + UUID.randomUUID();

  private static Process backend;
  private static URI apiBase;
  private static String testSchema;

  @BeforeAll static void startIsolatedBackend() throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
    apiBase = URI.create("http://127.0.0.1:" + port + "/api/v1");

    var envFile = loadDotEnvPostgres();
    String pgPort = firstNonBlank(System.getenv("POSTGRES_PORT"), envFile.get("POSTGRES_PORT"), "55432");
    String dbUser = firstNonBlank(System.getenv("DB_USER"), "ams");
    String dbPassword = firstNonBlank(System.getenv("DB_PASSWORD"), envFile.get("DB_PASSWORD"));
    assertTrue(dbPassword != null && !dbPassword.isBlank(),
      "PostgreSQL DB_PASSWORD is required (run scripts/start-postgres.ps1 to generate .env.postgres)");

    testSchema = "it_desktop_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String dbUrl = "jdbc:postgresql://127.0.0.1:" + pgPort + "/thapar_ams_test?currentSchema=" + testSchema;

    String javaExe = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String configuredCpFile = System.getProperty("ams.backend.classpathFile");
    ProcessBuilder builder;
    if (configuredCpFile != null && !configuredCpFile.isBlank()) {
      Path cpFile = Path.of(configuredCpFile).toAbsolutePath().normalize();
      assertTrue(Files.isRegularFile(cpFile), "Classpath file not found: " + cpFile);
      String classes = Path.of("..", "backend", "target", "classes").toAbsolutePath().normalize().toString();
      String cp = classes + java.io.File.pathSeparator + Files.readString(cpFile).trim();
      Path argFile = Files.createTempFile("ams-backend-cp-", ".args");
      argFile.toFile().deleteOnExit();
      Files.writeString(argFile, "-cp \"" + cp.replace("\\", "\\\\") + "\"");
      builder = new ProcessBuilder(
        javaExe, "@" + argFile, "com.thapar.ams.AmsApplication",
        "--server.port=" + port,
        "--spring.datasource.url=" + dbUrl,
        "--spring.flyway.default-schema=" + testSchema,
        "--spring.flyway.schemas=" + testSchema,
        "--spring.flyway.create-schemas=true",
        "--spring.jpa.properties.hibernate.default_schema=" + testSchema
      );
    } else {
      String configuredJar = System.getProperty("ams.backend.jar");
      Path jar = configuredJar != null && !configuredJar.isBlank()
        ? Path.of(configuredJar).toAbsolutePath().normalize()
        : Path.of("..", "backend", "target", "ams-1.0.0.jar").toAbsolutePath().normalize();
      assertTrue(Files.isRegularFile(jar), "Build backend/target/ams-1.0.0.jar before running DesktopBackendContractIT: " + jar);
      builder = new ProcessBuilder(
        javaExe, "-jar", jar.toString(),
        "--server.port=" + port,
        "--spring.datasource.url=" + dbUrl,
        "--spring.flyway.default-schema=" + testSchema,
        "--spring.flyway.schemas=" + testSchema,
        "--spring.flyway.create-schemas=true",
        "--spring.jpa.properties.hibernate.default_schema=" + testSchema
      );
    }
    var env = builder.environment();
    env.put("SPRING_PROFILES_ACTIVE", "local");
    env.put("DB_URL", dbUrl);
    env.put("DB_USER", dbUser);
    env.put("DB_PASSWORD", dbPassword);
    env.put("SEED_ENABLED", "true");
    env.put("SEED_FACULTY_ID", FACULTY_ID);
    env.put("SEED_STUDENT_ID", STUDENT_ID);
    env.put("SEED_FACULTY_PASSWORD", FACULTY_PASSWORD);
    env.put("SEED_STUDENT_PASSWORD", STUDENT_PASSWORD);
    builder.redirectErrorStream(true);
    Path logFile = Files.createTempFile("ams-it-backend-", ".log");
    logFile.toFile().deleteOnExit();
    builder.redirectOutput(logFile.toFile());
    backend = builder.start();

    var probe = new ApiClient(apiBase);
    long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
    while (System.nanoTime() < deadline) {
      if (!backend.isAlive()) {
        String output = Files.readString(logFile, StandardCharsets.UTF_8);
        fail("Isolated backend exited early with code " + backend.exitValue() + ":\n" + output);
      }
      try {
        probe.get("health");
        return;
      } catch (ApiClient.ApiException ignored) {
        Thread.sleep(300);
      }
    }
    fail("Timed out waiting for isolated backend on port " + port + ":\n" + Files.readString(logFile, StandardCharsets.UTF_8));
  }

  @AfterAll static void stopIsolatedBackend() {
    if (backend != null && backend.isAlive()) {
      backend.destroy();
      try {
        if (!backend.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) backend.destroyForcibly();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        backend.destroyForcibly();
      }
    }
    if (testSchema != null) {
      try {
        new ProcessBuilder(
          "docker", "exec", "thapar-ams-postgres-1",
          "psql", "-U", "ams", "-d", "thapar_ams_test",
          "-c", "DROP SCHEMA IF EXISTS \"" + testSchema + "\" CASCADE"
        ).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
      } catch (Exception ignored) {}
    }
  }

  private static java.util.Map<String, String> loadDotEnvPostgres() {
    var result = new java.util.HashMap<String, String>();
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path file = cwd.resolve(".env.postgres");
    if (!Files.isRegularFile(file) && cwd.getParent() != null) file = cwd.getParent().resolve(".env.postgres");
    if (!Files.isRegularFile(file)) return result;
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String t = line.trim();
        if (t.isEmpty() || t.startsWith("#")) continue;
        int eq = t.indexOf('=');
        if (eq > 0) result.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
      }
    } catch (IOException ignored) {}
    return result;
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) if (v != null && !v.isBlank()) return v.trim();
    return null;
  }

  @Test void exercisesAllDesktopClientsAgainstLiveBackend(@TempDir Path temp) throws Exception {
    var facultyClient = new ApiClient(apiBase);
    var studentClient = new ApiClient(apiBase);

    assertNull(facultyClient.session());
    var faculty = facultyClient.login(FACULTY_ID, FACULTY_PASSWORD, UserRole.FACULTY, false);
    var student = studentClient.login(STUDENT_ID, STUDENT_PASSWORD, UserRole.STUDENT, true);
    assertEquals(UserRole.FACULTY, faculty.role());
    assertEquals(UserRole.STUDENT, student.role());

    // Role isolation (403)
    var denied = assertThrows(ApiClient.ApiException.class, () -> studentClient.get("faculty/dashboard"));
    assertEquals(403, denied.status());

    // Dashboards & calendar
    var facultyWorkspace = new WorkspaceApi(facultyClient);
    var studentWorkspace = new WorkspaceApi(studentClient);
    assertTrue(facultyWorkspace.facultyDashboard().summary().activeClasses() > 0);
    assertTrue(studentWorkspace.studentDashboard().summary().registeredCourses() > 0);
    LocalDate eventDate = LocalDate.now(CAMPUS).plusDays(5);
    facultyWorkspace.addEvent("Contract IT Seminar", eventDate);
    assertTrue(studentWorkspace.studentEvents().stream()
      .anyMatch(e -> "Contract IT Seminar".equals(e.title()) && eventDate.equals(e.date())));

    // Courses, resource upload/download, announcement, and schedule edit
    var facultyCourses = new CourseApi(facultyClient);
    var studentCourses = new CourseApi(studentClient);
    var course = facultyCourses.facultyCourses().getFirst();
    facultyCourses.updateCourse(course.id(), course.title(), "Updated via contract IT");
    assertEquals("Updated via contract IT", studentCourses.studentCourses().stream()
      .filter(c -> c.id() == course.id()).findFirst().orElseThrow().description());

    Path resourceFile = temp.resolve("syllabus.txt");
    byte[] resourceBytes = "Contract IT resource bytes".getBytes(StandardCharsets.UTF_8);
    Files.write(resourceFile, resourceBytes);
    facultyCourses.upload(course.id(), resourceFile);
    var uploaded = facultyCourses.facultyResources(course.id()).stream()
      .filter(r -> "syllabus.txt".equals(r.title())).findFirst().orElseThrow();
    assertArrayEquals(resourceBytes, facultyCourses.facultyFile(uploaded.id()));
    assertArrayEquals(resourceBytes, studentCourses.studentFile(uploaded.id()));

    facultyCourses.announce(course.id(), "IT Notice", "Contract check");
    assertTrue(studentWorkspace.studentDashboard().announcements().stream()
      .anyMatch(a -> "IT Notice".equals(a.title())));

    // Attendance: move a session to now, issue QR token, claim, and verify 409 duplicate
    var session = facultyCourses.facultySchedule().getFirst();
    LocalDateTime now = LocalDateTime.now(CAMPUS).withSecond(0).withNano(0);
    facultyCourses.updateSession(session.id(), now.minusMinutes(5), now.plusMinutes(45), "LT-IT", false);
    var facultyAttendance = new AttendanceApi(facultyClient);
    var studentAttendance = new AttendanceApi(studentClient);
    var token = facultyAttendance.open(session.id());
    assertEquals(session.id(), token.sessionId());
    assertFalse(studentAttendance.claim(token.token()).message().isBlank());
    var duplicate = assertThrows(ApiClient.ApiException.class, () -> studentAttendance.claim(token.token()));
    assertEquals(409, duplicate.status());
    assertTrue(facultyAttendance.facultyRecords().stream().anyMatch(r -> r.sessionId() == session.id()));

    // Assessments: create, multipart submit, download, grade, publish, and results
    var facultyAssessments = new AssessmentApi(facultyClient);
    var studentAssessments = new AssessmentApi(studentClient);
    var created = facultyAssessments.create(course.id(), "Contract IT Lab", "Assignment", now.plusDays(2));
    Path workFile = temp.resolve("solution.txt");
    byte[] workBytes = "Student solution payload".getBytes(StandardCharsets.UTF_8);
    Files.write(workFile, workBytes);
    studentAssessments.submit(created.id(), "Written answer", workFile);

    var submitted = studentAssessments.studentAssessments().stream()
      .filter(a -> a.id() == created.id()).findFirst().orElseThrow();
    assertNotNull(submitted.submissionId());
    assertNull(submitted.marks());
    assertArrayEquals(workBytes, studentAssessments.studentFile(submitted.submissionId()));
    assertArrayEquals(workBytes, facultyAssessments.facultyFile(submitted.submissionId()));

    facultyAssessments.grade(submitted.submissionId(), 94, "Strong work");
    assertNull(studentAssessments.studentAssessments().stream()
      .filter(a -> a.id() == created.id()).findFirst().orElseThrow().marks());
    facultyAssessments.publish(created.id());
    var published = studentAssessments.results().results().stream()
      .filter(r -> r.id() == created.id()).findFirst().orElseThrow();
    assertEquals(94, published.marks());
    assertEquals("Strong work", published.feedback());

    // Messages
    var facultyMessages = new MessageApi(facultyClient, UserRole.FACULTY);
    var studentMessages = new MessageApi(studentClient, UserRole.STUDENT);
    facultyMessages.send(student.id(), "Hello from IT");
    studentMessages.send(faculty.id(), "Reply from IT");
    assertTrue(studentMessages.history().stream().anyMatch(m -> "Hello from IT".equals(m.body())));
    assertTrue(facultyMessages.history().stream().anyMatch(m -> "Reply from IT".equals(m.body())));

    // Account settings, notification read, password change + re-login, and logout
    var studentAccount = new AccountApi(studentClient, UserRole.STUDENT);
    var updated = studentAccount.updateProfile("IT Student Renamed", false);
    assertEquals("IT Student Renamed", updated.name());
    assertFalse(updated.notificationsEnabled());

    var notices = studentWorkspace.notifications("student");
    if (!notices.isEmpty()) studentWorkspace.markRead("student", notices.getFirst().id());

    String nextPassword = "Updated-" + UUID.randomUUID();
    studentAccount.changePassword(STUDENT_PASSWORD, nextPassword);
    var expired = assertThrows(ApiClient.ApiException.class, () -> studentClient.get("auth/me"));
    assertEquals(401, expired.status());

    var relogged = studentClient.login(STUDENT_ID, nextPassword, UserRole.STUDENT, false);
    assertEquals("IT Student Renamed", relogged.name());

    facultyClient.logout();
    studentClient.logout();
    assertNull(facultyClient.session());
  }
}
