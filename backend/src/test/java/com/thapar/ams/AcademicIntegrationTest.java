package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thapar.ams.entity.*;
import com.thapar.ams.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:ams-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "seed.enabled=true",
    "SEED_FACULTY_ID=test-faculty",
    "SEED_STUDENT_ID=test-student",
  }
)
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class AcademicIntegrationTest {

  private static final String FACULTY_PASSWORD = UUID.randomUUID().toString();
  private static final String STUDENT_PASSWORD = UUID.randomUUID().toString();

  @org.springframework.test.context.DynamicPropertySource
  static void generatedCredentials(
    org.springframework.test.context.DynamicPropertyRegistry registry
  ) {
    registry.add("SEED_FACULTY_PASSWORD", () -> FACULTY_PASSWORD);
    registry.add("SEED_STUDENT_PASSWORD", () -> STUDENT_PASSWORD);
  }

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  CourseRepository courses;

  @Autowired
  FacultyRepository faculty;

  @Autowired
  UserRepository users;

  @Autowired
  ClassSessionRepository sessions;

  MockHttpSession login(String role) throws Exception {
    boolean f = role.equals("FACULTY");
    return (MockHttpSession) mvc
      .perform(
        post("/api/v1/auth/login")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "identifier",
                f ? "test-faculty" : "test-student",
                "password",
                f ? FACULTY_PASSWORD : STUDENT_PASSWORD,
                "role",
                role
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn()
      .getRequest()
      .getSession(false);
  }

  @Test
  void anonymousSessionProbeDoesNotReturnAnAuthenticationError()
    throws Exception {
    mvc
      .perform(get("/api/v1/auth/session"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.authenticated").value(false));
    mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousCannotReadDashboard() throws Exception {
    mvc
      .perform(get("/api/v1/faculty/dashboard"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void csrfIsRequiredEvenForLogin() throws Exception {
    mvc
      .perform(
        post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void invalidCredentialsAndWrongRoleAreRejected() throws Exception {
    for (var body : List.of(
      Map.of(
        "identifier",
        "test-faculty",
        "password",
        UUID.randomUUID().toString(),
        "role",
        "FACULTY"
      ),
      Map.of(
        "identifier",
        "test-faculty",
        "password",
        FACULTY_PASSWORD,
        "role",
        "STUDENT"
      )
    ))
      mvc
        .perform(
          post("/api/v1/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))
        )
        .andExpect(status().isUnauthorized());
  }

  @Test
  void facultyCanReadRealDashboardAndLogout() throws Exception {
    var session = login("FACULTY");
    mvc
      .perform(get("/api/v1/auth/me").session(session))
      .andExpect(jsonPath("$.role").value("FACULTY"));
    mvc
      .perform(get("/api/v1/faculty/dashboard").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activeClasses").value(3))
      .andExpect(jsonPath("$.totalStudents").value(1));
    mvc
      .perform(get("/api/v1/faculty/schedule").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(3));
    mvc
      .perform(post("/api/v1/auth/logout").session(session).with(csrf()))
      .andExpect(status().isNoContent());
    assertTrue(session.isInvalid());
  }

  @Test
  void studentIsDeniedFacultyApi() throws Exception {
    var session = login("STUDENT");
    mvc
      .perform(get("/api/v1/faculty/students").session(session))
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/auth/me").session(session))
      .andExpect(jsonPath("$.role").value("STUDENT"));
  }

  @Test
  void facultyCannotAccessAnotherFacultysCourse() throws Exception {
    var u = new User();
    u.identifier = "other-faculty";
    u.name = "Synthetic Other Faculty";
    u.passwordHash =
      new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(
        UUID.randomUUID().toString()
      );
    u.role = Role.FACULTY;
    users.save(u);
    var f = new Faculty();
    f.user = u;
    faculty.save(f);
    var c = new Course();
    c.faculty = f;
    c.code = "OTHER-DEMO";
    c.title = "Restricted Course";
    c.section = "Other";
    courses.save(c);
    mvc
      .perform(
        get("/api/v1/faculty/courses/" + c.id + "/resources").session(
          login("FACULTY")
        )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void messagePersistsAndIsReturned() throws Exception {
    var session = login("FACULTY");
    var student = users.findByIdentifier("test-student").orElseThrow();
    mvc
      .perform(
        post("/api/v1/faculty/messages")
          .session(session)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "receiverId",
                student.id,
                "body",
                "Integration test message"
              )
            )
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/faculty/messages").session(session))
      .andExpect(status().isOk())
      .andExpect(
        content().string(
          org.hamcrest.Matchers.containsString("Integration test message")
        )
      );
  }

  @Test
  void attendanceTokenChecksIdentityAndRejectsReplay() throws Exception {
    var fac = faculty
      .findByUserId(users.findByIdentifier("test-faculty").orElseThrow().id)
      .orElseThrow();
    var course = courses.findByFacultyId(fac.id).getFirst();
    var c = new ClassSession();
    c.course = course;
    c.startsAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusMinutes(5);
    c.endsAt = c.startsAt.plusHours(1);
    c.room = "Demo Room";
    sessions.save(c);
    var res = mvc
      .perform(
        post("/api/v1/faculty/attendance/sessions")
          .session(login("FACULTY"))
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(Map.of("sessionId", c.id)))
      )
      .andExpect(status().isOk())
      .andReturn();
    var token = json
      .readTree(res.getResponse().getContentAsString())
      .get("token")
      .asText();
    var student = login("STUDENT");
    var payload = json.writeValueAsString(Map.of("token", token));
    mvc
      .perform(
        post("/api/v1/student/attendance/claim")
          .session(student)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(payload)
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/v1/student/attendance/claim")
          .session(student)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(payload)
      )
      .andExpect(status().isConflict());
  }

  @Test
  void browserCsrfTokenWorksAndRotatesAfterLogin() throws Exception {
    var first = mvc
      .perform(get("/api/v1/auth/csrf"))
      .andExpect(status().isOk())
      .andReturn();
    var session = (MockHttpSession) first.getRequest().getSession(false);
    var token = json
      .readTree(first.getResponse().getContentAsString())
      .get("token")
      .asText();
    mvc
      .perform(
        post("/api/v1/auth/login")
          .session(session)
          .header("X-CSRF-TOKEN", token)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "identifier",
                "test-faculty",
                "password",
                FACULTY_PASSWORD,
                "role",
                "FACULTY"
              )
            )
          )
      )
      .andExpect(status().isOk());
    var next = mvc
      .perform(get("/api/v1/auth/csrf").session(session))
      .andExpect(status().isOk())
      .andReturn();
    var rotated = json
      .readTree(next.getResponse().getContentAsString())
      .get("token")
      .asText();
    assertNotEquals(token, rotated);
    mvc
      .perform(
        post("/api/v1/auth/logout")
          .session(session)
          .header("X-CSRF-TOKEN", rotated)
      )
      .andExpect(status().isNoContent());
  }
}
