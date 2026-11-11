package com.thapar.ams.config;

import com.thapar.ams.entity.*;
import com.thapar.ams.security.LocalDevelopmentPolicy;
import com.thapar.ams.service.FacultyService;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local & !mysql & !staging & !prod & !production")
public class LocalFacultyDemoSeeder {

  private final FacultyService s;
  private final PasswordEncoder encoder;
  private final LocalDevelopmentPolicy policy;

  public LocalFacultyDemoSeeder(
    FacultyService s,
    PasswordEncoder encoder,
    LocalDevelopmentPolicy policy
  ) {
    this.s = s;
    this.encoder = encoder;
    this.policy = policy;
  }

  @Transactional
  public void seed(String identifier, String password, String displayName) {
    if (!policy.permitsDemoAccounts()) throw new IllegalStateException(
      "Demo provisioning requires exclusively the local profile and an H2 database"
    );
    if (
      identifier == null ||
      identifier.isBlank() ||
      identifier.length() > 120 ||
      password == null ||
      password.length() < 12 ||
      password.length() > 128 ||
      displayName == null ||
      displayName.isBlank() ||
      displayName.length() > 100
    ) throw new IllegalStateException("Invalid local demo seed configuration");
    var user = s.users().findByIdentifier(identifier).orElse(null);
    if (user != null) {
      if (
        user.role != Role.FACULTY ||
        !encoder.matches(password, user.passwordHash) ||
        !displayName.equals(user.name)
      ) throw new IllegalStateException(
        "Existing faculty demo identifier conflicts with the requested role, display name or credentials; no account was overwritten"
      );
    } else {
      user = new User();
      user.identifier = identifier;
      user.passwordHash = encoder.encode(password);
      user.name = displayName;
      user.role = Role.FACULTY;
      user.notificationsEnabled = true;
    }
    user.localOnly = true;
    s.users().save(user);
    var faculty = s.faculty().findByUserId(user.id).orElse(null);
    if (faculty == null) {
      faculty = new Faculty();
      faculty.user = user;
      faculty.department = "Computer Science & Engineering (local demo)";
      s.faculty().save(faculty);
    }
    var assigned = s.courses().findByFacultyId(faculty.id);
    String prefix = "LOCAL-" + faculty.id + "-";
    if (assigned.isEmpty()) {
      for (int i = 0; i < 3; i++) {
        var course = new Course();
        course.faculty = faculty;
        course.code = prefix + "CS" + (201 + i);
        course.title = i == 2 ? "Data Structures Lab" : "Data Structures";
        course.section = i == 1 ? "CSE 3B" : "CSE 3A";
        course.description =
          "Synthetic local demo course; not a real academic record.";
        s.courses().save(course);
      }
      assigned = s.courses().findByFacultyId(faculty.id);
    }
    var owned = assigned
      .stream()
      .filter(c -> c.code.startsWith(prefix))
      .toList();
    if (owned.isEmpty()) return; // Existing unrelated courses are never rewritten or populated with synthetic students.
    String studentId = "LOCAL-DEMO-STUDENT-" + faculty.id;
    var studentUser = s.users().findByIdentifier(studentId).orElse(null);
    if (studentUser == null) {
      studentUser = new User();
      studentUser.identifier = studentId;
      studentUser.name = "Synthetic Demo Student";
      studentUser.role = Role.STUDENT;
      studentUser.localOnly = true;
      studentUser.passwordHash = encoder.encode(UUID.randomUUID().toString());
      s.users().save(studentUser);
    } else if (
      studentUser.role != Role.STUDENT ||
      !studentUser.localOnly ||
      !studentUser.name.equals("Synthetic Demo Student")
    ) throw new IllegalStateException(
      "Synthetic student identifier conflict; no existing records were overwritten"
    );
    var student = s.students().findByUserId(studentUser.id).orElse(null);
    if (student == null) {
      student = new Student();
      student.user = studentUser;
      student.enrollmentNumber = "LOCAL-SYNTH-" + faculty.id;
      student.program = "B.E. Computer Science (synthetic local demo)";
      s.students().save(student);
    }
    var today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    int index = 0;
    for (var course : owned) {
      if (
        !s.enrollments().existsByStudentIdAndCourseId(student.id, course.id)
      ) {
        var e = new Enrollment();
        e.student = student;
        e.course = course;
        s.enrollments().save(e);
      }
      final int courseIndex = index++;
      var start = today.atTime(new int[] { 9, 12, 15 }[courseIndex % 3], 0);
      var schedules = s.sessions().findByCourseFacultyId(faculty.id);
      if (
        schedules
          .stream()
          .noneMatch(
            c -> c.course.id.equals(course.id) && c.startsAt.equals(start)
          )
      ) {
        var session = new ClassSession();
        session.course = course;
        session.startsAt = start;
        session.endsAt = start.plusHours(1);
        session.room = courseIndex == 2 ? "CS-Lab 1" : "LT-201";
        s.sessions().save(session);
      }
      var assessment = s
        .assessments()
        .findByCourseFacultyId(faculty.id)
        .stream()
        .filter(a -> a.course.id.equals(course.id))
        .findFirst()
        .orElse(null);
      if (assessment == null) {
        assessment = new Assessment();
        assessment.course = course;
        assessment.title =
          courseIndex == 0
            ? "Demo DSA problem set"
            : "Demo practice quiz " + courseIndex;
        assessment.kind = courseIndex == 0 ? "Assignment" : "Quiz";
        assessment.deadline = today.plusDays(2 + courseIndex).atTime(23, 59);
        s.assessments().save(assessment);
      }
      if (courseIndex == 0) {
        var pastStart = today.minusDays(1).atTime(9, 0);
        var past = s
          .sessions()
          .findByCourseFacultyId(faculty.id)
          .stream()
          .filter(
            v -> v.course.id.equals(course.id) && v.startsAt.equals(pastStart)
          )
          .findFirst()
          .orElse(null);
        if (past == null) {
          past = new ClassSession();
          past.course = course;
          past.startsAt = pastStart;
          past.endsAt = pastStart.plusHours(1);
          past.room = "LT-201";
          s.sessions().save(past);
        }
        if (
          !s
            .attendance()
            .existsByStudentIdAndClassSessionId(student.id, past.id)
        ) {
          var record = new AttendanceRecord();
          record.student = student;
          record.classSession = past;
          record.recordedAt = pastStart.plusMinutes(5);
          s.attendance().save(record);
        }
        var assessmentId = assessment.id;
        var studentPk = student.id;
        if (
          s
            .submissions()
            .findByAssessmentCourseFacultyId(faculty.id)
            .stream()
            .noneMatch(
              v ->
                v.assessment.id.equals(assessmentId) &&
                v.student.id.equals(studentPk)
            )
        ) {
          var submission = new Submission();
          submission.student = student;
          submission.assessment = assessment;
          submission.content =
            "Synthetic local demo submission for the faculty review workflow.";
          submission.submittedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
          s.submissions().save(submission);
        }
      }
    }
    if (s.notifications().findByUserId(user.id).isEmpty()) {
      var n = new Notification();
      n.user = user;
      n.title =
        "Local demo workspace: all associated example records are synthetic.";
      s.notifications().save(n);
    }
    if (s.events().findByFacultyId(faculty.id).isEmpty()) {
      var event = new CalendarEvent();
      event.faculty = faculty;
      event.title = "Synthetic faculty planning meeting";
      event.eventDate = today.plusDays(2);
      s.events().save(event);
    }
  }
}
