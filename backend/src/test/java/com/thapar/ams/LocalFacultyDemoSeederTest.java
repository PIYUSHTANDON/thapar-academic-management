package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;

import com.thapar.ams.config.LocalFacultyDemoSeeder;
import com.thapar.ams.entity.*;
import com.thapar.ams.service.FacultyService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:local-demo-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "seed.enabled=false",
  }
)
@Transactional
class LocalFacultyDemoSeederTest {

  @Autowired
  LocalFacultyDemoSeeder seed;

  @Autowired
  FacultyService s;

  @Autowired
  PasswordEncoder encoder;

  final String password = UUID.randomUUID().toString();

  @Test
  void createsBcryptLocalFacultyAndSyntheticTeachingData() {
    seed.seed("FACULTY001", password, "Demo Faculty");
    var user = s.users().findByIdentifier("FACULTY001").orElseThrow();
    assertEquals(Role.FACULTY, user.role);
    assertEquals("Demo Faculty", user.name);
    assertTrue(user.localOnly);
    assertTrue(user.passwordHash.startsWith("$2"));
    assertTrue(encoder.matches(password, user.passwordHash));
    assertNotEquals(password, user.passwordHash);
    var f = s.faculty().findByUserId(user.id).orElseThrow();
    assertEquals(3, s.courses().findByFacultyId(f.id).size());
    assertEquals(4, s.sessions().findByCourseFacultyId(f.id).size());
    assertEquals(
      1,
      s.attendance().findByClassSessionCourseFacultyId(f.id).size()
    );
    assertEquals(3, s.assessments().findByCourseFacultyId(f.id).size());
  }

  @Test
  void rerunningDoesNotOverwriteHashOrDuplicateRecords() {
    seed.seed("FACULTY001", password, "Demo Faculty");
    var before = s
      .users()
      .findByIdentifier("FACULTY001")
      .orElseThrow()
      .passwordHash;
    long courses = s.courses().count(),
      students = s.students().count(),
      sessions = s.sessions().count(),
      attendance = s.attendance().count();
    seed.seed("FACULTY001", password, "Demo Faculty");
    assertEquals(
      before,
      s.users().findByIdentifier("FACULTY001").orElseThrow().passwordHash
    );
    assertEquals(courses, s.courses().count());
    assertEquals(students, s.students().count());
    assertEquals(sessions, s.sessions().count());
    assertEquals(attendance, s.attendance().count());
  }

  @Test
  void credentialConflictLeavesExistingAccountUnchanged() {
    var u = new User();
    u.identifier = "FACULTY001";
    u.name = "Existing Faculty";
    u.role = Role.FACULTY;
    u.passwordHash = encoder.encode(UUID.randomUUID().toString());
    s.users().save(u);
    var original = u.passwordHash;
    assertThrows(IllegalStateException.class, () ->
      seed.seed("FACULTY001", password, "Demo Faculty")
    );
    assertEquals(original, u.passwordHash);
    assertEquals("Existing Faculty", u.name);
    assertFalse(u.localOnly);
    assertEquals(0, s.courses().count());
  }

  @Test
  void wrongRoleIsNotOverwritten() {
    var u = new User();
    u.identifier = "FACULTY001";
    u.name = "Demo Faculty";
    u.role = Role.STUDENT;
    u.passwordHash = encoder.encode(password);
    s.users().save(u);
    assertThrows(IllegalStateException.class, () ->
      seed.seed("FACULTY001", password, "Demo Faculty")
    );
    assertEquals(Role.STUDENT, u.role);
    assertEquals(0, s.faculty().count());
  }

  @Test
  void unrelatedFacultyAndStudentRecordsArePreserved() {
    var facultyUser = new User();
    facultyUser.identifier = "unrelated-faculty";
    facultyUser.name = "Existing faculty";
    facultyUser.role = Role.FACULTY;
    facultyUser.passwordHash = encoder.encode(UUID.randomUUID().toString());
    s.users().save(facultyUser);
    var f = new Faculty();
    f.user = facultyUser;
    f.department = "Existing department";
    s.faculty().save(f);
    var studentUser = new User();
    studentUser.identifier = "unrelated-student";
    studentUser.name = "Existing student";
    studentUser.role = Role.STUDENT;
    studentUser.passwordHash = encoder.encode(UUID.randomUUID().toString());
    s.users().save(studentUser);
    var st = new Student();
    st.user = studentUser;
    st.enrollmentNumber = "EXISTING-1";
    st.program = "Existing program";
    s.students().save(st);
    var facultyHash = facultyUser.passwordHash;
    var studentHash = studentUser.passwordHash;
    seed.seed("FACULTY001", password, "Demo Faculty");
    assertEquals(
      facultyHash,
      s.users().findByIdentifier("unrelated-faculty").orElseThrow().passwordHash
    );
    assertEquals(
      studentHash,
      s.users().findByIdentifier("unrelated-student").orElseThrow().passwordHash
    );
    assertEquals(
      "Existing department",
      s.faculty().findByUserId(facultyUser.id).orElseThrow().department
    );
    assertEquals(
      "EXISTING-1",
      s.students().findByUserId(studentUser.id).orElseThrow().enrollmentNumber
    );
  }
}
