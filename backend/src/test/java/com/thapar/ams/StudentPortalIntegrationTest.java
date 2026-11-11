package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thapar.ams.entity.*;
import com.thapar.ams.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:student-portal;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "seed.enabled=true",
    "SEED_FACULTY_ID=student-test-faculty",
    "SEED_STUDENT_ID=student-test-student",
  }
)
@AutoConfigureMockMvc
@Transactional
class StudentPortalIntegrationTest {

  static final String FACULTY_PASSWORD = UUID.randomUUID().toString(),
    STUDENT_PASSWORD = UUID.randomUUID().toString();

  @DynamicPropertySource
  static void credentials(DynamicPropertyRegistry registry) {
    String fp = FACULTY_PASSWORD,
      sp = STUDENT_PASSWORD;
    registry.add("SEED_FACULTY_PASSWORD", () -> fp);
    registry.add("SEED_STUDENT_PASSWORD", () -> sp);
  }

  @Autowired
  MockMvc mvc;

  @Autowired
  FacultyService s;

  @Autowired
  StudentService students;

  @Autowired
  ObjectMapper json;

  @Autowired
  PasswordEncoder encoder;

  org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor student() {
    return user("student-test-student").roles("STUDENT");
  }

  org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor faculty() {
    return user("student-test-faculty").roles("FACULTY");
  }

  Course course() {
    return s
      .courses()
      .findByFacultyId(s.faculty("student-test-faculty").id)
      .getFirst();
  }

  Assessment assignment() {
    var a = new Assessment();
    a.course = course();
    a.title = "Student integration assignment";
    a.kind = "Assignment";
    a.deadline = StudentService.now().plusDays(4);
    return s.assessments().save(a);
  }

  Long submit(Assessment a) throws Exception {
    var r = mvc
      .perform(
        multipart("/api/v1/student/submissions")
          .file(
            new MockMultipartFile(
              "file",
              "work.txt",
              "text/plain",
              "Synthetic student work".getBytes()
            )
          )
          .param("assessmentId", a.id.toString())
          .param("content", "A student response")
          .with(student())
          .with(csrf())
      )
      .andExpect(status().isOk())
      .andReturn();
    return json
      .readTree(r.getResponse().getContentAsString())
      .get("id")
      .asLong();
  }

  User otherUser(Role role) {
    var u = new User();
    u.identifier = "other-" + UUID.randomUUID();
    u.name = "Synthetic other user";
    u.role = role;
    u.passwordHash = encoder.encode(UUID.randomUUID().toString());
    return s.users().save(u);
  }

  @Test
  void dashboardUsesStoredCgpaAndRealPendingAssignments() throws Exception {
    assignment();
    mvc
      .perform(get("/api/v1/student/dashboard").with(student()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.registeredCourses").value(3))
      .andExpect(jsonPath("$.pendingAssignments").value(1))
      .andExpect(
        jsonPath("$.currentCgpa").value(org.hamcrest.Matchers.nullValue())
      );
    var st = students.student("student-test-student");
    st.officialCgpa = new BigDecimal("7.89");
    s.students().save(st);
    mvc
      .perform(get("/api/v1/student/dashboard").with(student()))
      .andExpect(jsonPath("$.currentCgpa").value(7.89));
  }

  @Test
  void anonymousFacultyAndOtherCourseAccessAreDenied() throws Exception {
    mvc
      .perform(get("/api/v1/student/dashboard"))
      .andExpect(status().isUnauthorized());
    mvc
      .perform(get("/api/v1/student/dashboard").with(faculty()))
      .andExpect(status().isForbidden());
    var f = new Faculty();
    f.user = otherUser(Role.FACULTY);
    s.faculty().save(f);
    var c = new Course();
    c.faculty = f;
    c.code = "PRIVATE-COURSE";
    c.title = "Private";
    s.courses().save(c);
    mvc
      .perform(
        get("/api/v1/student/courses/" + c.id + "/resources").with(student())
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/student/courses").with(student()))
      .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void submittedFileIsVisibleToOwningFacultyAndOwnStudent() throws Exception {
    var a = assignment();
    var id = submit(a);
    mvc
      .perform(get("/api/v1/faculty/submissions").with(faculty()))
      .andExpect(
        content().string(org.hamcrest.Matchers.containsString("work.txt"))
      );
    mvc
      .perform(
        get("/api/v1/student/submissions/" + id + "/file").with(student())
      )
      .andExpect(content().bytes("Synthetic student work".getBytes()));
    mvc
      .perform(
        get("/api/v1/faculty/submissions/" + id + "/file").with(faculty())
      )
      .andExpect(status().isOk());
  }

  @Test
  void gradesAndFeedbackStayHiddenUntilPublished() throws Exception {
    var a = assignment();
    var id = submit(a);
    mvc
      .perform(
        post("/api/v1/faculty/submissions/" + id + "/grade")
          .with(faculty())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"marks\":87,\"feedback\":\"Clear reasoning\"}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/results").with(student()))
      .andExpect(jsonPath("$.results.length()").value(0));
    mvc
      .perform(
        post("/api/v1/faculty/assessments/" + a.id + "/publish")
          .with(faculty())
          .with(csrf())
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/results").with(student()))
      .andExpect(jsonPath("$.results[0].marks").value(87))
      .andExpect(jsonPath("$.results[0].feedback").value("Clear reasoning"));
    mvc
      .perform(
        post("/api/v1/faculty/submissions/" + id + "/grade")
          .with(faculty())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"marks\":90}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/results").with(student()))
      .andExpect(jsonPath("$.results.length()").value(0));
  }

  @Test
  void anotherStudentsSubmissionCannotBeDownloaded() throws Exception {
    var a = assignment();
    var st = new Student();
    st.user = otherUser(Role.STUDENT);
    st.enrollmentNumber = "PRIVATE-STUDENT";
    s.students().save(st);
    var sub = new Submission();
    sub.student = st;
    sub.assessment = a;
    sub.content = "private content";
    sub.fileContent = "private".getBytes();
    sub.fileName = "private.txt";
    s.submissions().save(sub);
    mvc
      .perform(
        get("/api/v1/student/submissions/" + sub.id + "/file").with(student())
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/student/submissions").with(student()))
      .andExpect(
        content().string(
          org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("private content")
          )
        )
      );
  }

  @Test
  void expiredEmptyUnsupportedForgedAndOversizedSubmissionsAreRejected()
    throws Exception {
    var a = assignment();
    for (var file : List.of(
      new MockMultipartFile(
        "file",
        "bad.exe",
        "application/octet-stream",
        "MZ".getBytes()
      ),
      new MockMultipartFile(
        "file",
        "bad.pdf",
        "application/pdf",
        "not a pdf".getBytes()
      ),
      new MockMultipartFile(
        "file",
        "huge.txt",
        "text/plain",
        new byte[5 * 1024 * 1024 + 1]
      )
    )) {
      mvc
        .perform(
          multipart("/api/v1/student/submissions")
            .file(file)
            .param("assessmentId", a.id.toString())
            .with(student())
            .with(csrf())
        )
        .andExpect(status().isBadRequest());
    }
    mvc
      .perform(
        multipart("/api/v1/student/submissions")
          .param("assessmentId", a.id.toString())
          .with(student())
          .with(csrf())
      )
      .andExpect(status().isBadRequest());
    a.deadline = StudentService.now().minusDays(1);
    s.assessments().save(a);
    mvc
      .perform(
        multipart("/api/v1/student/submissions")
          .param("assessmentId", a.id.toString())
          .param("content", "Late")
          .with(student())
          .with(csrf())
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateSubmissionAndCsrfBypassAreRejected() throws Exception {
    var a = assignment();
    submit(a);
    mvc
      .perform(
        multipart("/api/v1/student/submissions")
          .param("assessmentId", a.id.toString())
          .param("content", "Replay")
          .with(student())
          .with(csrf())
      )
      .andExpect(status().isConflict());
    mvc
      .perform(
        post("/api/v1/student/messages")
          .with(student())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void messagesSynchronizeInBothDirectionsAndRejectUnrelatedRecipients()
    throws Exception {
    var teacher = s.user("student-test-faculty");
    mvc
      .perform(
        post("/api/v1/student/messages")
          .with(student())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of("receiverId", teacher.id, "body", "Student to teacher")
            )
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/faculty/messages").with(faculty()))
      .andExpect(
        content().string(
          org.hamcrest.Matchers.containsString("Student to teacher")
        )
      );
    mvc
      .perform(
        post("/api/v1/faculty/messages")
          .with(faculty())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "receiverId",
                students.student("student-test-student").user.id,
                "body",
                "Teacher reply"
              )
            )
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/messages").with(student()))
      .andExpect(
        content().string(org.hamcrest.Matchers.containsString("Teacher reply"))
      );
    mvc
      .perform(
        post("/api/v1/student/messages")
          .with(student())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "receiverId",
                otherUser(Role.FACULTY).id,
                "body",
                "Unauthorized"
              )
            )
          )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void materialAndAnnouncementsAreSharedOnlyWithEnrolledStudent()
    throws Exception {
    var c = course();
    mvc
      .perform(
        multipart("/api/v1/faculty/courses/" + c.id + "/resources")
          .file(
            new MockMultipartFile(
              "file",
              "notes.txt",
              "text/plain",
              "Course notes".getBytes()
            )
          )
          .with(faculty())
          .with(csrf())
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/v1/faculty/courses/" + c.id + "/announcements")
          .with(faculty())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"title\":\"Synthetic notice\",\"description\":\"Full notice details\"}"
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/announcements").with(student()))
      .andExpect(
        content().string(
          org.hamcrest.Matchers.containsString("Full notice details")
        )
      );
    mvc
      .perform(
        get("/api/v1/student/courses/" + c.id + "/resources").with(student())
      )
      .andExpect(
        content().string(org.hamcrest.Matchers.containsString("notes.txt"))
      );
  }

  @Test
  void facultySessionEditsAppearInStudentTimetable() throws Exception {
    var session = s
      .sessions()
      .findByCourseFacultyId(s.faculty("student-test-faculty").id)
      .getFirst();
    mvc
      .perform(
        put("/api/v1/faculty/schedule/" + session.id)
          .with(faculty())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "startsAt",
                session.startsAt.toString(),
                "endsAt",
                session.endsAt.toString(),
                "room",
                "QA-LT-999",
                "cancelled",
                false
              )
            )
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/student/schedule").with(student()))
      .andExpect(
        content().string(org.hamcrest.Matchers.containsString("QA-LT-999"))
      );
  }

  @Test
  void inactiveEnrollmentDeniesResourcesAndQrClaims() throws Exception {
    var c = course();
    var enrollment = s
      .enrollments()
      .findByStudentIdAndActiveTrue(students.student("student-test-student").id)
      .stream()
      .filter(e -> e.course.id.equals(c.id))
      .findFirst()
      .orElseThrow();
    enrollment.active = false;
    s.enrollments().save(enrollment);
    mvc
      .perform(
        get("/api/v1/student/courses/" + c.id + "/resources").with(student())
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/student/courses").with(student()))
      .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void notificationOwnershipAndAcademicFieldsAreProtected() throws Exception {
    var n = new Notification();
    n.user = s.user("student-test-faculty");
    n.title = "Private faculty notification";
    s.notifications().save(n);
    mvc
      .perform(
        post("/api/v1/student/notifications/" + n.id + "/read")
          .with(student())
          .with(csrf())
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        put("/api/v1/student/settings/profile")
          .with(student())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"name\":\"Demo Student\",\"notificationsEnabled\":false,\"officialCgpa\":10}"
          )
      )
      .andExpect(status().isOk());
    assertNull(students.student("student-test-student").officialCgpa);
  }

  @Test
  void studentPasswordChangeRequiresCurrentPasswordAndHashesNewPassword()
    throws Exception {
    String next = UUID.randomUUID().toString();
    mvc
      .perform(
        put("/api/v1/student/settings/password")
          .with(student())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "currentPassword",
                UUID.randomUUID().toString(),
                "newPassword",
                next
              )
            )
          )
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        put("/api/v1/student/settings/password")
          .with(student())
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of("currentPassword", STUDENT_PASSWORD, "newPassword", next)
            )
          )
      )
      .andExpect(status().isOk());
    assertTrue(
      encoder.matches(
        next,
        students.student("student-test-student").user.passwordHash
      )
    );
  }
}
