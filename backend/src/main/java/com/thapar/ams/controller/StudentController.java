package com.thapar.ams.controller;

import com.thapar.ams.entity.*;
import com.thapar.ams.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentController {

  private final FacultyService s;

  private final StudentService students;
  private final SubmissionService submissions;
  private final PasswordEncoder encoder;

  public StudentController(
    FacultyService s,
    StudentService students,
    SubmissionService submissions,
    PasswordEncoder encoder
  ) {
    this.s = s;
    this.students = students;
    this.submissions = submissions;
    this.encoder = encoder;
  }

  @GetMapping("/dashboard")
  Object dashboard(Principal p) {
    return students.dashboard(p.getName());
  }

  @GetMapping("/profile")
  Object profile(Principal p) {
    return students.profile(p.getName());
  }

  @GetMapping("/courses")
  Object courses(Principal p) {
    return students.courses(p.getName());
  }

  @GetMapping("/schedule")
  Object schedule(Principal p) {
    return students.schedule(p.getName());
  }

  @GetMapping("/attendance")
  Object attendance(Principal p) {
    return students.attendance(p.getName());
  }

  @GetMapping("/assessments")
  Object assessments(Principal p) {
    return students.assessments(p.getName());
  }

  @GetMapping("/results")
  Object results(Principal p) {
    return StudentService.dto(
      "officialCgpa",
      students.student(p.getName()).officialCgpa,
      "results",
      students.results(p.getName())
    );
  }

  @GetMapping("/calendar")
  Object calendar(Principal p) {
    return students.calendar(p.getName());
  }

  @GetMapping("/announcements")
  Object announcements(Principal p) {
    return students.announcements(p.getName());
  }

  @GetMapping("/courses/{id}/resources")
  Object resources(Principal p, @PathVariable Long id) {
    return students.resources(p.getName(), id);
  }

  @GetMapping("/resources/{id}")
  ResponseEntity<byte[]> resource(Principal p, @PathVariable Long id) {
    var r = s.resources().findById(id).orElseThrow();
    students.course(p.getName(), r.course.id);
    if (!"File".equals(r.kind)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    return attachment(r.content, "resource-" + id);
  }

  static ResponseEntity<byte[]> attachment(byte[] data, String filename) {
    return ResponseEntity.ok()
      .header(
        "Content-Disposition",
        ContentDisposition.attachment()
          .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
          .build()
          .toString()
      )
      .header("X-Content-Type-Options", "nosniff")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(data);
  }

  @GetMapping("/submissions")
  Object submissions(Principal p) {
    return students
      .assessments(p.getName())
      .stream()
      .filter(a -> a.get("submissionId") != null)
      .toList();
  }

  @PostMapping(
    value = "/submissions",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  Object submit(
    Principal p,
    @RequestParam Long assessmentId,
    @RequestParam(required = false) String content,
    @RequestParam(required = false) MultipartFile file
  ) throws Exception {
    var submission = submissions.submit(
      p.getName(),
      assessmentId,
      content,
      file
    );
    return Map.of("id", submission.id, "message", "Assignment submitted");
  }

  @GetMapping("/submissions/{id}/file")
  ResponseEntity<byte[]> submissionFile(Principal p, @PathVariable Long id) {
    var sub = s.submissions().findById(id).orElseThrow();
    if (
      !sub.student.id.equals(students.student(p.getName()).id)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    students.course(p.getName(), sub.assessment.course.id);
    if (sub.fileContent == null) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    return attachment(sub.fileContent, sub.fileName);
  }

  @GetMapping("/messages/contacts")
  Object contacts(Principal p) {
    return students.teachers(p.getName());
  }

  @GetMapping("/messages")
  Object messages(Principal p) {
    return students.messages(p.getName());
  }

  record MessageInput(
    @NotNull Long receiverId,
    @NotBlank @Size(max = 4000) String body
  ) {}

  @PostMapping("/messages")
  Object message(Principal p, @Valid @RequestBody MessageInput b) {
    if (
      !students.canMessage(p.getName(), b.receiverId)
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "You can message only teachers of enrolled courses"
    );
    var message = new Message();
    message.sender = students.student(p.getName()).user;
    message.receiver = s.users().findById(b.receiverId).orElseThrow();
    message.body = b.body;
    message.sentAt = StudentService.now();
    s.messages().save(message);
    return Map.of("id", message.id);
  }

  @GetMapping("/notifications")
  Object notifications(Principal p) {
    return s
      .notifications()
      .findByUserId(students.student(p.getName()).user.id)
      .stream()
      .map(n -> Map.of("id", n.id, "title", n.title, "read", n.read))
      .toList();
  }

  @PostMapping("/notifications/{id}/read")
  void read(Principal p, @PathVariable Long id) {
    var n = s.notifications().findById(id).orElseThrow();
    if (
      !n.user.id.equals(students.student(p.getName()).user.id)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    n.read = true;
    s.notifications().save(n);
  }

  @GetMapping("/search")
  Object search(Principal p, @RequestParam(defaultValue = "") String q) {
    if (q.length() > 128) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST
    );
    String term = q.toLowerCase(Locale.ROOT).trim();
    if (term.isEmpty()) return List.of();
    var results = new ArrayList<Map<String, Object>>();
    for (var c : students.courses(p.getName()))
      if (
        (c.get("title") + " " + c.get("code"))
          .toLowerCase(Locale.ROOT)
          .contains(term)
      ) results.add(
        Map.of(
          "label",
          c.get("title"),
          "kind",
          "Course",
          "path",
          "/student/courses"
        )
      );
    for (var r : s
      .resources()
      .findByCourseIdIn(students.courseIds(p.getName())))
      if (
        (r.title + " " + Objects.toString(r.description, ""))
          .toLowerCase(Locale.ROOT)
          .contains(term)
      ) results.add(
        Map.of("label", r.title, "kind", r.kind, "path", "/student/courses")
      );
    return results.stream().limit(12).toList();
  }

  record ProfileInput(
    @NotBlank @Size(max = 100) String name,
    boolean notificationsEnabled
  ) {}

  @PutMapping("/settings/profile")
  void updateProfile(Principal p, @Valid @RequestBody ProfileInput b) {
    var user = students.student(p.getName()).user;
    user.name = b.name;
    user.notificationsEnabled = b.notificationsEnabled;
    s.users().save(user);
  }

  record PasswordInput(
    @NotBlank @Size(max = 128) String currentPassword,
    @NotBlank @Size(min = 12, max = 128) String newPassword
  ) {
    @Override
    public String toString() {
      return "PasswordInput[REDACTED]";
    }
  }

  @PutMapping("/settings/password")
  void password(
    Principal p,
    @Valid @RequestBody PasswordInput b,
    jakarta.servlet.http.HttpServletRequest request
  ) {
    var user = students.student(p.getName()).user;
    if (
      !encoder.matches(b.currentPassword, user.passwordHash)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Current password is incorrect"
    );
    user.passwordHash = encoder.encode(b.newPassword);
    s.users().save(user);
    request.getSession().invalidate();
  }

  record Claim(@NotBlank String token) {}

  @PostMapping("/attendance/claim")
  Object claim(Principal p, @Valid @RequestBody Claim b) {
    var student = s
      .students()
      .findByUserId(s.user(p.getName()).id)
      .orElseThrow();
    var session = s
      .sessions()
      .findByTokenHash(FacultyService.hash(b.token))
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid attendance token"
        )
      );
    students.course(p.getName(), session.course.id);
    if (
      session.cancelled ||
      session.endsAt.isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))) ||
      session.tokenExpiresAt.isBefore(
        LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
      ) ||
      !s
        .enrollments()
        .existsByStudentIdAndCourseId(student.id, session.course.id)
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Expired token or student is not enrolled"
    );
    if (
      s.attendance().existsByStudentIdAndClassSessionId(student.id, session.id)
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Attendance already recorded"
    );
    var r = new AttendanceRecord();
    r.student = student;
    r.classSession = session;
    r.recordedAt = LocalDateTime.now();
    s.attendance().save(r);
    return Map.of("message", "Attendance recorded for authenticated student");
  }
}
