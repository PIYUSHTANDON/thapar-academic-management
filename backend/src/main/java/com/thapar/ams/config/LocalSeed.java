package com.thapar.ams.config;

import com.thapar.ams.entity.*;
import com.thapar.ams.service.*;
import java.time.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("local & !mysql & !staging & !prod & !production")
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")
public class LocalSeed implements ApplicationRunner {

  final FacultyService s;
  final PasswordEncoder encoder;
  final Environment env;
  final LocalFacultyDemoSeeder demoSeeder;
  final LocalStudentDemoSeeder studentDemoSeeder;

  public LocalSeed(
    FacultyService s,
    PasswordEncoder e,
    Environment env,
    LocalFacultyDemoSeeder demoSeeder,
    LocalStudentDemoSeeder studentDemoSeeder
  ) {
    this.s = s;
    encoder = e;
    this.env = env;
    this.demoSeeder = demoSeeder;
    this.studentDemoSeeder = studentDemoSeeder;
  }

  String required(String key) {
    var v = env.getProperty(key);
    if (v == null || v.isBlank()) throw new IllegalStateException(
      "Local seeding requires " + key
    );
    return v;
  }

  User user(String id, String password, String name, Role role) {
    if (password.length() < 12) throw new IllegalStateException(
      "Seed passwords require at least 12 characters"
    );
    var u = new User();
    u.identifier = id;
    u.passwordHash = encoder.encode(password);
    u.name = name;
    u.role = role;
    u.notificationsEnabled = true;
    return s.users().save(u);
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if ("student-demo".equals(env.getProperty("seed.mode"))) {
      studentDemoSeeder.seed(
        required("SEED_STUDENT_ID"),
        required("SEED_STUDENT_PASSWORD"),
        env.getProperty("SEED_STUDENT_NAME", "Demo Student"),
        env.getProperty("SEED_FACULTY_ID", "FACULTY001")
      );
      return;
    }
    if ("faculty-demo".equals(env.getProperty("seed.mode"))) {
      demoSeeder.seed(
        required("SEED_FACULTY_ID"),
        required("SEED_FACULTY_PASSWORD"),
        env.getProperty("SEED_FACULTY_NAME", "Demo Faculty")
      );
      return;
    }
    String fid = required("SEED_FACULTY_ID"),
      fp = required("SEED_FACULTY_PASSWORD"),
      sid = required("SEED_STUDENT_ID"),
      sp = required("SEED_STUDENT_PASSWORD");
    if (s.users().findByIdentifier(fid).isPresent()) return;
    var f = new Faculty();
    f.user = user(fid, fp, "Dr. Aarya Mehta (Demo)", Role.FACULTY);
    f.department = "Computer Science & Engineering";
    s.faculty().save(f);
    var st = new Student();
    st.user = user(sid, sp, "Demo Student", Role.STUDENT);
    st.enrollmentNumber = "SYNTH-001";
    st.academicYear = 3;
    st.semester = 5;
    st.program = "B.E. Computer Science (synthetic)";
    s.students().save(st);
    var today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    for (int i = 0; i < 3; i++) {
      var c = new Course();
      c.faculty = f;
      c.code = "DEMO-CS" + (201 + i);
      c.title = i == 2 ? "Data Structures Lab" : "Data Structures";
      c.section = i == 1 ? "CSE 3B" : "CSE 3A";
      c.description = "Synthetic local development course";
      s.courses().save(c);
      var en = new Enrollment();
      en.student = st;
      en.course = c;
      s.enrollments().save(en);
      var cl = new ClassSession();
      cl.course = c;
      cl.startsAt = today.atTime(new int[] { 9, 12, 15 }[i], 0);
      cl.endsAt = cl.startsAt.plusHours(1);
      cl.room = i == 2 ? "CS-Lab 1" : "LT-201";
      s.sessions().save(cl);
      var a = new Assessment();
      a.course = c;
      a.title = i == 0 ? "DSA problem set" : "Practice quiz " + i;
      a.kind = i == 0 ? "Assignment" : "Quiz";
      a.deadline = today.plusDays(i + 2).atTime(23, 59);
      s.assessments().save(a);
      if (i == 0) {
        var sub = new Submission();
        sub.assessment = a;
        sub.student = st;
        sub.content = "Synthetic example submission for grading.";
        sub.submittedAt = LocalDateTime.now();
        s.submissions().save(sub);
      }
    }
    var event = new CalendarEvent();
    event.faculty = f;
    event.title = "Department academic review";
    event.eventDate = today.plusDays(2);
    s.events().save(event);
    var n = new Notification();
    n.user = f.user;
    n.title = "Welcome to your faculty workspace. Local data is synthetic.";
    s.notifications().save(n);
    var m = new Message();
    m.sender = st.user;
    m.receiver = f.user;
    m.body =
      "Hello Professor, could you clarify the problem set deadline? (Demo message)";
    m.sentAt = LocalDateTime.now();
    s.messages().save(m);
  }
}
