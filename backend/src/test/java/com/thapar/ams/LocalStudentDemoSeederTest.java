package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;

import com.thapar.ams.config.*;
import com.thapar.ams.entity.*;
import com.thapar.ams.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:student-demo-seed;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "seed.enabled=false",
  }
)
@Transactional
class LocalStudentDemoSeederTest {

  @Autowired
  LocalFacultyDemoSeeder facultySeed;

  @Autowired
  LocalStudentDemoSeeder studentSeed;

  @Autowired
  FacultyService s;

  @Autowired
  StudentService students;

  @Autowired
  PasswordEncoder encoder;

  String fp = UUID.randomUUID().toString(),
    sp = UUID.randomUUID().toString();

  void faculty() {
    facultySeed.seed("DEMO-FACULTY", fp, "Demo Faculty");
  }

  @Test
  void createsLocalOnlyStudentWithBcryptAndSharedEnrollments() {
    faculty();
    studentSeed.seed("DEMO-STUDENT", sp, "Demo Student", "DEMO-FACULTY");
    var u = s.users().findByIdentifier("DEMO-STUDENT").orElseThrow();
    assertTrue(u.localOnly);
    assertEquals(Role.STUDENT, u.role);
    assertTrue(encoder.matches(sp, u.passwordHash));
    assertEquals(3, students.enrolled(u.identifier).size());
    assertNull(students.student(u.identifier).officialCgpa);
  }

  @Test
  void rerunIsIdempotentAndDoesNotChangeFacultyCredentials() {
    faculty();
    String facultyHash = s.user("DEMO-FACULTY").passwordHash;
    studentSeed.seed("DEMO-STUDENT", sp, "Demo Student", "DEMO-FACULTY");
    String hash = s.user("DEMO-STUDENT").passwordHash;
    long users = s.users().count(),
      enrollments = s.enrollments().count(),
      resources = s.resources().count();
    studentSeed.seed("DEMO-STUDENT", sp, "Demo Student", "DEMO-FACULTY");
    assertEquals(hash, s.user("DEMO-STUDENT").passwordHash);
    assertEquals(facultyHash, s.user("DEMO-FACULTY").passwordHash);
    assertEquals(users, s.users().count());
    assertEquals(enrollments, s.enrollments().count());
    assertEquals(resources, s.resources().count());
  }

  @Test
  void conflictingStudentPasswordIsNotOverwritten() {
    faculty();
    studentSeed.seed("DEMO-STUDENT", sp, "Demo Student", "DEMO-FACULTY");
    String hash = s.user("DEMO-STUDENT").passwordHash;
    assertThrows(IllegalStateException.class, () ->
      studentSeed.seed(
        "DEMO-STUDENT",
        UUID.randomUUID().toString(),
        "Demo Student",
        "DEMO-FACULTY"
      )
    );
    assertEquals(hash, s.user("DEMO-STUDENT").passwordHash);
  }

  @Test
  void ordinaryFacultyCannotBePopulatedByDemoSeeder() {
    var u = new User();
    u.identifier = "ORDINARY-FACULTY";
    u.role = Role.FACULTY;
    u.name = "Existing faculty";
    u.passwordHash = encoder.encode(UUID.randomUUID().toString());
    s.users().save(u);
    assertThrows(IllegalStateException.class, () ->
      studentSeed.seed("DEMO-STUDENT", sp, "Demo Student", u.identifier)
    );
    assertTrue(s.users().findByIdentifier("DEMO-STUDENT").isEmpty());
  }
}
