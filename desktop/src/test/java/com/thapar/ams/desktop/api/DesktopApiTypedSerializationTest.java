package com.thapar.ams.desktop.api;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thapar.ams.contract.AuthContracts.UserRole;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopApiTypedSerializationTest {
  private HttpServer server;
  private ApiClient client;

  @BeforeEach void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    Map<String, String> fixtures = Map.ofEntries(
      Map.entry("/api/v1/auth/csrf", "{\"token\":\"t\",\"headerName\":\"X-CSRF-TOKEN\"}"),
      Map.entry("/api/v1/auth/me",
        "{\"id\":2,\"name\":\"Aarav\",\"identifier\":\"STU01\",\"role\":\"STUDENT\",\"notificationsEnabled\":true}"),
      Map.entry("/api/v1/student/profile",
        "{\"id\":10,\"name\":\"Aarav\",\"identifier\":\"STU01\",\"enrollmentNumber\":\"1023001\",\"program\":\"B.E. CSE\",\"academicYear\":2,\"semester\":4,\"officialCgpa\":null,\"notificationsEnabled\":true,\"synthetic\":true}"),
      Map.entry("/api/v1/student/dashboard",
        "{\"overallAttendance\":null,\"currentCgpa\":null,\"pendingAssignments\":2,\"registeredCourses\":3}"),
      Map.entry("/api/v1/student/schedule",
        "[{\"id\":5,\"courseId\":1,\"course\":\"Algorithms - CS1\",\"startsAt\":\"2026-11-19T10:00:00\",\"endsAt\":\"2026-11-19T11:00:00\",\"room\":\"LT-101\",\"status\":\"Upcoming\"}]"),
      Map.entry("/api/v1/student/assessments",
        "[{\"id\":9,\"title\":\"HW1\",\"courseId\":1,\"course\":\"Algorithms\",\"kind\":\"Assignment\",\"deadline\":\"2026-11-25T23:59:00\",\"status\":\"Pending\",\"submissionId\":null,\"submittedAt\":null,\"fileName\":null,\"content\":null,\"marks\":null,\"feedback\":null}]"),
      Map.entry("/api/v1/student/announcements",
        "[{\"id\":3,\"title\":\"Welcome\",\"kind\":\"Announcement\",\"description\":\"Read syllabus\",\"publishedAt\":null,\"courseId\":1,\"course\":\"Algorithms\",\"faculty\":\"Dr. Ada\"}]"),
      Map.entry("/api/v1/student/calendar",
        "[{\"id\":\"event-1\",\"title\":\"Midsem\",\"date\":\"2026-11-28\",\"kind\":\"Event\"}]"),
      Map.entry("/api/v1/student/notifications",
        "[{\"id\":4,\"title\":\"UCS301: Welcome\",\"read\":false}]"),
      Map.entry("/api/v1/student/results",
        "{\"officialCgpa\":null,\"results\":[]}"),
      Map.entry("/api/v1/student/attendance",
        "{\"percentage\":null,\"attended\":0,\"total\":0,\"courses\":[{\"courseId\":1,\"course\":\"Algorithms\",\"section\":\"CS1\",\"attended\":0,\"total\":0,\"missed\":0,\"percentage\":null}],\"records\":[]}"),
      Map.entry("/api/v1/faculty/students",
        "[{\"id\":10,\"userId\":2,\"name\":\"Aarav\",\"enrollmentNumber\":\"1023001\",\"program\":\"B.E. CSE\",\"courseId\":1,\"course\":\"Algorithms\"},{\"id\":10,\"userId\":2,\"name\":\"Aarav\",\"enrollmentNumber\":\"1023001\",\"program\":\"B.E. CSE\",\"courseId\":2,\"course\":\"OS\"}]"),
      Map.entry("/api/v1/faculty/messages",
        "[{\"id\":8,\"senderId\":1,\"sender\":\"Dr. Ada\",\"receiverId\":2,\"receiver\":\"Aarav\",\"body\":\"Hello\",\"sentAt\":\"2026-11-19T09:15:00\"}]"),
      Map.entry("/api/v1/faculty/courses/1/resources",
        "[{\"id\":6,\"title\":\"slides.pdf\",\"kind\":\"File\"}]")
    );
    server.createContext("/api/v1/", exchange -> {
      String body = fixtures.getOrDefault(exchange.getRequestURI().getPath(), "{}");
      reply(exchange, 200, body);
    });
    server.start();
    client = new ApiClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1"));
  }

  @AfterEach void stop() {
    if (server != null) server.stop(0);
  }

  @Test void deserializesNullableStudentAndFacultyContractsCleanly() {
    var workspace = new WorkspaceApi(client).studentDashboard();
    assertNull(workspace.summary().overallAttendance());
    assertNull(workspace.summary().currentCgpa());
    assertEquals(LocalDate.of(2026, 11, 28), workspace.events().getFirst().date());
    assertEquals(LocalDateTime.of(2026, 11, 19, 10, 0), workspace.schedule().getFirst().startsAt());
    assertNull(workspace.assessments().getFirst().marks());
    assertNull(workspace.announcements().getFirst().publishedAt());

    var attendance = new AttendanceApi(client).studentSummary();
    assertNull(attendance.percentage());
    assertNull(attendance.courses().getFirst().percentage());

    var results = new AssessmentApi(client).results();
    assertNull(results.officialCgpa());
    assertTrue(results.results().isEmpty());

    var resources = new CourseApi(client).facultyResources(1L);
    assertEquals("slides.pdf", resources.getFirst().title());
    assertNull(resources.getFirst().description());

    var profile = new AccountApi(client, UserRole.STUDENT).load();
    assertEquals("1023001", profile.student().enrollmentNumber());
    assertNull(profile.student().officialCgpa());

    var messages = new MessageApi(client, UserRole.FACULTY);
    assertEquals(1, messages.contacts().size(), "Duplicate enrollments must collapse to one contact");
    assertEquals("1023001", messages.contacts().getFirst().detail());
    assertEquals(LocalDateTime.of(2026, 11, 19, 9, 15), messages.history().getFirst().sentAt());
  }

  @Test void enforcesClientSideValidationGuards(@TempDir Path temp) throws Exception {
    assertThrows(IllegalArgumentException.class, () -> new AccountApi(client, UserRole.ADMIN));
    assertThrows(IllegalArgumentException.class, () -> new MessageApi(client, UserRole.ADMIN));

    var assessments = new AssessmentApi(client);
    assertThrows(IllegalArgumentException.class, () -> assessments.submit(1L, "   ", null));
    assertThrows(IllegalArgumentException.class, () -> assessments.submit(1L, "a".repeat(4001), null));
    Path exe = temp.resolve("bad.exe");
    Files.writeString(exe, "nope");
    assertThrows(IllegalArgumentException.class, () -> assessments.submit(1L, "ok", exe));

    Path oversized = temp.resolve("big.txt");
    Files.write(oversized, new byte[5 * 1024 * 1024 + 1]);
    assertThrows(IllegalArgumentException.class, () -> assessments.submit(1L, "", oversized));

    var courses = new CourseApi(client);
    assertThrows(IllegalArgumentException.class, () -> courses.upload(1L, temp.resolve("missing.txt")));
    Path hugeResource = temp.resolve("huge.bin");
    Files.write(hugeResource, new byte[10 * 1024 * 1024 + 1]);
    assertThrows(IllegalArgumentException.class, () -> courses.upload(1L, hugeResource));
  }

  private static void reply(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
