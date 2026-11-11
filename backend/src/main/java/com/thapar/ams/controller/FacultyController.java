package com.thapar.ams.controller;

import com.thapar.ams.entity.*;
import com.thapar.ams.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Transactional
@RequestMapping("/api/v1/faculty")
public class FacultyController {

  private final FacultyService s;
  private final PasswordEncoder encoder;

  public FacultyController(FacultyService s, PasswordEncoder e) {
    this.s = s;
    encoder = e;
  }

  @GetMapping("/dashboard")
  Object dashboard(Principal p) {
    return s.dashboard(p.getName());
  }

  @GetMapping("/courses")
  Object courses(Principal p) {
    return s.courses(p.getName());
  }

  @GetMapping("/students")
  Object students(Principal p) {
    return s.roster(p.getName());
  }

  @GetMapping("/schedule")
  Object schedule(Principal p) {
    return s.schedule(p.getName());
  }

  @GetMapping("/calendar")
  Object calendar(Principal p) {
    return s.calendar(p.getName());
  }

  @GetMapping("/notifications")
  Object notifications(Principal p) {
    return s
      .notifications()
      .findByUserId(s.user(p.getName()).id)
      .stream()
      .map(n -> Map.of("id", n.id, "title", n.title, "read", n.read))
      .toList();
  }

  @PostMapping("/notifications/{id}/read")
  void read(Principal p, @PathVariable Long id) {
    var n = s.notifications().findById(id).orElseThrow();
    if (
      !n.user.id.equals(s.user(p.getName()).id)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    n.read = true;
    s.notifications().save(n);
  }

  @GetMapping("/messages")
  Object messages(Principal p) {
    return s.messages(p.getName());
  }

  record MessageInput(
    @NotNull Long receiverId,
    @NotBlank @Size(max = 4000) String body
  ) {}

  @PostMapping("/messages")
  Object send(Principal p, @Valid @RequestBody MessageInput b) {
    if (
      !s.canMessage(p.getName(), b.receiverId)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    var m = new Message();
    m.sender = s.user(p.getName());
    m.receiver = s.users().findById(b.receiverId).orElseThrow();
    m.body = b.body;
    m.sentAt = LocalDateTime.now();
    s.messages().save(m);
    return Map.of("id", m.id);
  }

  @GetMapping("/assessments")
  Object assessments(Principal p) {
    return s.assessments(p.getName());
  }

  record AssessmentInput(
    @NotNull Long courseId,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Pattern(regexp = "Assignment|Quiz") String kind,
    @NotNull @Future LocalDateTime deadline
  ) {}

  @PostMapping("/assessments")
  Object createAssessment(Principal p, @Valid @RequestBody AssessmentInput b) {
    var a = new Assessment();
    a.course = s.course(p.getName(), b.courseId);
    a.title = b.title;
    a.kind = b.kind;
    a.deadline = b.deadline;
    s.assessments().save(a);
    return Map.of("id", a.id);
  }

  @GetMapping("/submissions")
  Object submissions(Principal p) {
    return s
      .submissions()
      .findByAssessmentCourseFacultyId(s.faculty(p.getName()).id)
      .stream()
      .map(v -> {
        var grade = s
          .grades()
          .findAll()
          .stream()
          .filter(g -> g.submission.id.equals(v.id))
          .findFirst();
        return StudentService.dto(
          "id",
          v.id,
          "assessment",
          v.assessment.title,
          "student",
          v.student.user.name,
          "content",
          v.content,
          "marks",
          grade.map(g -> g.marks.toString()).orElse("Ungraded"),
          "feedback",
          grade.map(g -> Objects.toString(g.feedback, "")).orElse(""),
          "fileName",
          v.fileName
        );
      })
      .toList();
  }

  record Mark(@Min(0) @Max(100) int marks, @Size(max = 2000) String feedback) {}

  @PostMapping("/submissions/{id}/grade")
  void grade(Principal p, @PathVariable Long id, @Valid @RequestBody Mark b) {
    var sub = s.submissions().findById(id).orElseThrow();
    s.course(p.getName(), sub.assessment.course.id);
    var g = s
      .grades()
      .findAll()
      .stream()
      .filter(x -> x.submission.id.equals(id))
      .findFirst()
      .orElseGet(Grade::new);
    g.submission = sub;
    g.marks = b.marks;
    if (b.feedback != null) g.feedback = b.feedback;
    g.published = false;
    s.grades().save(g);
  }

  @PostMapping("/assessments/{id}/publish")
  void publish(Principal p, @PathVariable Long id) {
    var a = s.assessments().findById(id).orElseThrow();
    s.course(p.getName(), a.course.id);
    a.published = true;
    s.assessments().save(a);
    s.grades()
      .findAll()
      .stream()
      .filter(g -> g.submission.assessment.id.equals(id))
      .forEach(g -> {
        g.published = true;
        s.grades().save(g);
      });
  }

  record EventInput(
    @NotBlank @Size(max = 255) String title,
    @NotNull LocalDate eventDate
  ) {}

  @PostMapping("/calendar")
  void event(Principal p, @Valid @RequestBody EventInput b) {
    var e = new CalendarEvent();
    e.faculty = s.faculty(p.getName());
    e.title = b.title;
    e.eventDate = b.eventDate;
    s.events().save(e);
  }

  @GetMapping("/attendance")
  Object attendance(Principal p) {
    return s
      .attendance()
      .findByClassSessionCourseFacultyId(s.faculty(p.getName()).id)
      .stream()
      .map(a ->
        Map.of(
          "id",
          a.id,
          "student",
          a.student.user.name,
          "sessionId",
          a.classSession.id,
          "course",
          a.classSession.course.title,
          "recordedAt",
          a.recordedAt
        )
      )
      .toList();
  }

  record SessionInput(@NotNull Long sessionId) {}

  @PostMapping("/attendance/sessions")
  Object token(Principal p, @Valid @RequestBody SessionInput b) {
    var v = s.sessions().findById(b.sessionId).orElseThrow();
    s.course(p.getName(), v.course.id);
    var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    if (
      v.cancelled ||
      now.isBefore(v.startsAt.minusMinutes(15)) ||
      now.isAfter(v.endsAt)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Attendance opens 15 minutes before class and closes at its end"
    );
    var bytes = new byte[32];
    new java.security.SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes);
    v.tokenHash = FacultyService.hash(token);
    v.tokenExpiresAt = now.plusMinutes(3);
    s.sessions().save(v);
    return Map.of(
      "token",
      token,
      "expiresAt",
      v.tokenExpiresAt,
      "sessionId",
      v.id
    );
  }

  record CourseInput(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description
  ) {}

  @PutMapping("/courses/{id}")
  void course(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody CourseInput b
  ) {
    var c = s.course(p.getName(), id);
    c.title = b.title;
    c.description = b.description;
    s.courses().save(c);
  }

  @GetMapping("/courses/{id}/resources")
  Object resources(Principal p, @PathVariable Long id) {
    s.course(p.getName(), id);
    return s
      .resources()
      .findByCourseId(id)
      .stream()
      .map(r -> Map.of("id", r.id, "title", r.title, "kind", r.kind))
      .toList();
  }

  @PostMapping("/courses/{id}/resources")
  void upload(
    Principal p,
    @PathVariable Long id,
    @RequestParam MultipartFile file
  ) throws Exception {
    var r = new LearningResource();
    r.course = s.course(p.getName(), id);
    r.title = Objects.toString(file.getOriginalFilename(), "Resource");
    if (r.title.length() > 255) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST
    );
    r.kind = "File";
    r.content = file.getBytes();
    r.contentType = "application/octet-stream";
    s.resources().save(r);
  }

  @GetMapping("/resources/{id}")
  ResponseEntity<byte[]> download(Principal p, @PathVariable Long id) {
    var r = s.resources().findById(id).orElseThrow();
    s.course(p.getName(), r.course.id);
    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=resource-" + id)
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(r.content);
  }

  record Announcement(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 4000) String description
  ) {}

  @PostMapping("/courses/{id}/announcements")
  void announce(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody Announcement b
  ) {
    var c = s.course(p.getName(), id);
    var r = new LearningResource();
    r.course = c;
    r.title = b.title;
    r.kind = "Announcement";
    r.description = b.description;
    s.resources().save(r);
    s.enrollments()
      .findByCourseId(id)
      .forEach(e -> {
        var n = new Notification();
        n.user = e.student.user;
        n.title = c.code + ": " + b.title;
        s.notifications().save(n);
      });
  }

  record Profile(
    @NotBlank @Size(max = 100) String name,
    boolean notificationsEnabled
  ) {}

  @PutMapping("/settings/profile")
  void profile(Principal p, @Valid @RequestBody Profile b) {
    var u = s.user(p.getName());
    u.name = b.name;
    u.notificationsEnabled = b.notificationsEnabled;
    s.users().save(u);
  }

  record Password(
    @NotBlank @Size(max = 128) String currentPassword,
    @NotBlank @Size(min = 12, max = 128) String newPassword
  ) {
    @Override
    public String toString() {
      return "Password[REDACTED]";
    }
  }

  @PutMapping("/settings/password")
  void password(
    Principal p,
    @Valid @RequestBody Password b,
    jakarta.servlet.http.HttpServletRequest request
  ) {
    var u = s.user(p.getName());
    if (
      !encoder.matches(b.currentPassword, u.passwordHash)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Current password is incorrect"
    );
    u.passwordHash = encoder.encode(b.newPassword);
    s.users().save(u);
    request.getSession().invalidate();
  }

  @GetMapping("/submissions/{id}/file")
  ResponseEntity<byte[]> submissionFile(Principal p, @PathVariable Long id) {
    var sub = s.submissions().findById(id).orElseThrow();
    s.course(p.getName(), sub.assessment.course.id);
    if (sub.fileContent == null) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND
    );
    return StudentController.attachment(sub.fileContent, sub.fileName);
  }

  record ScheduleInput(
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt,
    @NotBlank @Size(max = 100) String room,
    boolean cancelled
  ) {}

  @PutMapping("/schedule/{id}")
  void updateSession(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody ScheduleInput b
  ) {
    var session = s.sessions().findById(id).orElseThrow();
    s.course(p.getName(), session.course.id);
    if (!b.endsAt.isAfter(b.startsAt)) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Class end must be after its start"
    );
    session.startsAt = b.startsAt;
    session.endsAt = b.endsAt;
    session.room = b.room;
    session.cancelled = b.cancelled;
    session.tokenHash = null;
    session.tokenExpiresAt = null;
    s.sessions().save(session);
  }
}
