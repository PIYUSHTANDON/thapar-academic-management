package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thapar.ams.contract.AccountContracts.ChangePasswordRequest;
import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AccountContracts.StudentProfile;
import com.thapar.ams.contract.AssessmentContracts.FacultyAssessment;
import com.thapar.ams.contract.AssessmentContracts.FacultySubmission;
import com.thapar.ams.contract.AssessmentContracts.GradeRequest;
import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.AssessmentContracts.StudentResults;
import com.thapar.ams.contract.AttendanceContracts.FacultyAttendanceRecord;
import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceSummary;
import com.thapar.ams.contract.AuthContracts.ApiErrorResponse;
import com.thapar.ams.contract.AuthContracts.CsrfTokenResponse;
import com.thapar.ams.contract.AuthContracts.SessionResponse;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.contract.CourseContracts.SearchResult;
import com.thapar.ams.contract.CourseContracts.StudentCourse;
import com.thapar.ams.contract.CourseContracts.UpdateCourseRequest;
import com.thapar.ams.contract.MessageContracts.ContactItem;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.FacultyDashboard;
import com.thapar.ams.contract.OverviewContracts.StudentDashboard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
  properties = {
    "seed.enabled=true",
    "SEED_FACULTY_ID=contract-faculty",
    "SEED_STUDENT_ID=contract-student",
  }
)
@AutoConfigureMockMvc
@Transactional
class ContractSerializationTest {
  private static final String FACULTY_PASSWORD = UUID.randomUUID().toString();
  private static final String STUDENT_PASSWORD = UUID.randomUUID().toString();

  private final ObjectMapper strictDesktopMapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @DynamicPropertySource
  static void credentials(DynamicPropertyRegistry registry) {
    PostgresTestSupport.register(registry, "contract");
    registry.add("SEED_FACULTY_PASSWORD", () -> FACULTY_PASSWORD);
    registry.add("SEED_STUDENT_PASSWORD", () -> STUDENT_PASSWORD);
  }

  @Autowired MockMvc mvc;

  @Test void authResponsesMatchContractsAndPreserveOptionalNullOmission() throws Exception {
    String csrfBody = mvc.perform(get("/api/v1/auth/csrf"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var csrfToken = strictDesktopMapper.readValue(csrfBody, CsrfTokenResponse.class);
    assertFalse(csrfToken.token().isBlank());
    assertFalse(csrfToken.headerName().isBlank());

    String anonBody = mvc.perform(get("/api/v1/auth/session"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertFalse(strictDesktopMapper.readTree(anonBody).has("user"));
    var anon = strictDesktopMapper.readValue(anonBody, SessionResponse.class);
    assertFalse(anon.authenticated());
    assertNull(anon.user());

    String meBody = mvc.perform(get("/api/v1/auth/me").with(user("contract-faculty").roles("FACULTY")))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var me = strictDesktopMapper.readValue(meBody, UserResponse.class);
    assertEquals(UserRole.FACULTY, me.role());
    assertEquals("contract-faculty", me.identifier());
  }

  @Test void allFacultyReadEndpointsDeserializeStrictlyIntoContracts() throws Exception {
    var faculty = user("contract-faculty").roles("FACULTY");

    var dashboard = read(mvc.perform(get("/api/v1/faculty/dashboard").with(faculty)), FacultyDashboard.class);
    assertTrue(dashboard.activeClasses() > 0);
    assertNotNull(dashboard.actions());

    var courses = readList(mvc.perform(get("/api/v1/faculty/courses").with(faculty)), new TypeReference<List<FacultyCourse>>() { });
    assertFalse(courses.isEmpty());

    var roster = readList(mvc.perform(get("/api/v1/faculty/students").with(faculty)), new TypeReference<List<RosterEntry>>() { });
    assertFalse(roster.isEmpty());

    var schedule = readList(mvc.perform(get("/api/v1/faculty/schedule").with(faculty)), new TypeReference<List<ClassSessionItem>>() { });
    assertFalse(schedule.isEmpty());
    assertNotNull(schedule.getFirst().startsAt());

    String calendarJson = mvc.perform(get("/api/v1/faculty/calendar").with(faculty))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var calendar = strictDesktopMapper.readValue(calendarJson, new TypeReference<List<CalendarEvent>>() { });
    assertFalse(calendar.isEmpty());
    assertFalse(strictDesktopMapper.readTree(calendarJson).get(0).has("kind"));

    String resourcesJson = mvc.perform(get("/api/v1/faculty/courses/" + courses.getFirst().id() + "/resources").with(faculty))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var resources = strictDesktopMapper.readValue(resourcesJson, new TypeReference<List<ResourceItem>>() { });
    assertNotNull(resources);

    var assessments = readList(mvc.perform(get("/api/v1/faculty/assessments").with(faculty)), new TypeReference<List<FacultyAssessment>>() { });
    assertFalse(assessments.isEmpty());

    var submissions = readList(mvc.perform(get("/api/v1/faculty/submissions").with(faculty)), new TypeReference<List<FacultySubmission>>() { });
    assertFalse(submissions.isEmpty());

    var attendance = readList(mvc.perform(get("/api/v1/faculty/attendance").with(faculty)), new TypeReference<List<FacultyAttendanceRecord>>() { });
    assertNotNull(attendance);

    var messages = readList(mvc.perform(get("/api/v1/faculty/messages").with(faculty)), new TypeReference<List<MessageItem>>() { });
    assertFalse(messages.isEmpty());

    var notifications = readList(mvc.perform(get("/api/v1/faculty/notifications").with(faculty)), new TypeReference<List<NotificationItem>>() { });
    assertFalse(notifications.isEmpty());
  }

  @Test void allStudentReadEndpointsDeserializeStrictlyIntoContracts() throws Exception {
    var student = user("contract-student").roles("STUDENT");

    var dashboard = read(mvc.perform(get("/api/v1/student/dashboard").with(student)), StudentDashboard.class);
    assertTrue(dashboard.registeredCourses() > 0);

    var profile = read(mvc.perform(get("/api/v1/student/profile").with(student)), StudentProfile.class);
    assertEquals("contract-student", profile.identifier());

    var courses = readList(mvc.perform(get("/api/v1/student/courses").with(student)), new TypeReference<List<StudentCourse>>() { });
    assertFalse(courses.isEmpty());

    var schedule = readList(mvc.perform(get("/api/v1/student/schedule").with(student)), new TypeReference<List<ClassSessionItem>>() { });
    assertFalse(schedule.isEmpty());

    var announcements = readList(mvc.perform(get("/api/v1/student/announcements").with(student)), new TypeReference<List<ResourceItem>>() { });
    assertNotNull(announcements);

    var resources = readList(mvc.perform(get("/api/v1/student/courses/" + courses.getFirst().id() + "/resources").with(student)),
      new TypeReference<List<ResourceItem>>() { });
    assertNotNull(resources);

    var search = readList(mvc.perform(get("/api/v1/student/search?q=Data").with(student)), new TypeReference<List<SearchResult>>() { });
    assertNotNull(search);

    var calendar = readList(mvc.perform(get("/api/v1/student/calendar").with(student)), new TypeReference<List<CalendarEvent>>() { });
    assertFalse(calendar.isEmpty());
    assertNotNull(calendar.getFirst().kind());

    var attendance = read(mvc.perform(get("/api/v1/student/attendance").with(student)), StudentAttendanceSummary.class);
    assertFalse(attendance.courses().isEmpty());

    var assessments = readList(mvc.perform(get("/api/v1/student/assessments").with(student)), new TypeReference<List<StudentAssessment>>() { });
    assertFalse(assessments.isEmpty());

    var results = read(mvc.perform(get("/api/v1/student/results").with(student)), StudentResults.class);
    assertNotNull(results.results());

    var submissions = readList(mvc.perform(get("/api/v1/student/submissions").with(student)), new TypeReference<List<StudentAssessment>>() { });
    assertNotNull(submissions);

    var contacts = readList(mvc.perform(get("/api/v1/student/messages/contacts").with(student)), new TypeReference<List<ContactItem>>() { });
    assertFalse(contacts.isEmpty());

    var messages = readList(mvc.perform(get("/api/v1/student/messages").with(student)), new TypeReference<List<MessageItem>>() { });
    assertFalse(messages.isEmpty());

    var notifications = readList(mvc.perform(get("/api/v1/student/notifications").with(student)), new TypeReference<List<NotificationItem>>() { });
    assertNotNull(notifications);
  }

  @Test void jakartaValidationConstraintsOnSharedContractsRejectInvalidPayloads() throws Exception {
    var faculty = user("contract-faculty").roles("FACULTY");

    String invalidMarks = mvc.perform(post("/api/v1/faculty/submissions/1/grade")
        .with(faculty).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(strictDesktopMapper.writeValueAsString(new GradeRequest(101, "Too high"))))
      .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    var marksError = strictDesktopMapper.readValue(invalidMarks, ApiErrorResponse.class);
    assertNotNull(marksError.errors());
    assertTrue(marksError.errors().stream().anyMatch(e -> e.startsWith("marks:")));

    String shortPassword = mvc.perform(put("/api/v1/faculty/settings/password")
        .with(faculty).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(strictDesktopMapper.writeValueAsString(new ChangePasswordRequest(FACULTY_PASSWORD, "short"))))
      .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    var passwordError = strictDesktopMapper.readValue(shortPassword, ApiErrorResponse.class);
    assertNotNull(passwordError.errors());
    assertTrue(passwordError.errors().stream().anyMatch(e -> e.startsWith("newPassword:")));

    String blankTitle = mvc.perform(put("/api/v1/faculty/courses/1")
        .with(faculty).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(strictDesktopMapper.writeValueAsString(new UpdateCourseRequest("   ", "desc"))))
      .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
    var titleError = strictDesktopMapper.readValue(blankTitle, ApiErrorResponse.class);
    assertNotNull(titleError.errors());
    assertTrue(titleError.errors().stream().anyMatch(e -> e.startsWith("title:")));
  }

  private <T> T read(org.springframework.test.web.servlet.ResultActions actions, Class<T> type) throws Exception {
    String body = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return strictDesktopMapper.readValue(body, type);
  }

  private <T> T readList(org.springframework.test.web.servlet.ResultActions actions, TypeReference<T> type) throws Exception {
    String body = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return strictDesktopMapper.readValue(body, type);
  }
}
