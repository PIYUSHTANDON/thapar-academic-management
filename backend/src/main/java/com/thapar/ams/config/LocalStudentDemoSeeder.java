package com.thapar.ams.config;

import com.thapar.ams.entity.*;
import com.thapar.ams.security.LocalDevelopmentPolicy;
import com.thapar.ams.service.FacultyService;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local & !mysql & !staging & !prod & !production")
public class LocalStudentDemoSeeder {

  private final FacultyService s;
  private final PasswordEncoder encoder;
  private final LocalDevelopmentPolicy policy;

  public LocalStudentDemoSeeder(
    FacultyService s,
    PasswordEncoder encoder,
    LocalDevelopmentPolicy policy
  ) {
    this.s = s;
    this.encoder = encoder;
    this.policy = policy;
  }

  @Transactional
  public void seed(
    String identifier,
    String password,
    String name,
    String facultyIdentifier
  ) {
    if (!policy.permitsDemoAccounts()) throw new IllegalStateException(
      "Student demo provisioning requires the sole local profile and H2"
    );
    if (
      identifier == null ||
      identifier.isBlank() ||
      identifier.length() > 120 ||
      password == null ||
      password.length() < 12 ||
      password.length() > 128 ||
      name == null ||
      name.isBlank() ||
      name.length() > 100
    ) throw new IllegalStateException(
      "Invalid student demo seed configuration"
    );
    var teacher = s
      .users()
      .findByIdentifier(facultyIdentifier)
      .orElseThrow(() ->
        new IllegalStateException("Provision the local Faculty demo first")
      );
    if (
      teacher.role != Role.FACULTY || !teacher.localOnly
    ) throw new IllegalStateException(
      "Student demos may be connected only to an existing local-only Faculty demo"
    );
    var faculty = s.faculty().findByUserId(teacher.id).orElseThrow();
    var courses = s
      .courses()
      .findByFacultyId(faculty.id)
      .stream()
      .filter(c ->
        Objects.toString(c.description, "")
          .toLowerCase(Locale.ROOT)
          .contains("synthetic")
      )
      .toList();
    if (courses.isEmpty()) throw new IllegalStateException(
      "No synthetic local demo courses exist for this faculty"
    );
    var user = s.users().findByIdentifier(identifier).orElse(null);
    if (user != null) {
      if (
        user.role != Role.STUDENT ||
        !user.localOnly ||
        !name.equals(user.name) ||
        !encoder.matches(password, user.passwordHash)
      ) throw new IllegalStateException(
        "Existing student identifier conflicts; nothing was overwritten"
      );
    } else {
      user = new User();
      user.identifier = identifier;
      user.name = name;
      user.role = Role.STUDENT;
      user.localOnly = true;
      user.notificationsEnabled = true;
      user.passwordHash = encoder.encode(password);
      s.users().save(user);
    }
    var student = s.students().findByUserId(user.id).orElse(null);
    if (student == null) {
      student = new Student();
      student.user = user;
      student.enrollmentNumber = "LOCAL-STUDENT-" + user.id;
      student.program =
        "B.Tech. Computer Science & Engineering (synthetic local demo)";
      student.academicYear = 3;
      student.semester = 5;
      s.students().save(student);
    }
    for (var course : courses) {
      if (
        !s.enrollments().existsByStudentIdAndCourseId(student.id, course.id)
      ) {
        var e = new Enrollment();
        e.student = student;
        e.course = course;
        s.enrollments().save(e);
      }
      if (
        s
          .resources()
          .findByCourseId(course.id)
          .stream()
          .noneMatch(r -> "Announcement".equals(r.kind))
      ) {
        var r = new LearningResource();
        r.course = course;
        r.kind = "Announcement";
        r.title = "Demo: learning plan for " + course.code;
        r.description =
          "Synthetic example announcement. Review your course materials and upcoming assessment deadlines in this local workspace.";
        s.resources().save(r);
      }
    }
  }
}
