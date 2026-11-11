package com.thapar.ams.service;

import com.thapar.ams.entity.*;
import com.thapar.ams.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyService {

  private final UserRepository users;
  private final FacultyRepository faculty;
  private final StudentRepository students;
  private final CourseRepository courses;
  private final EnrollmentRepository enrollments;
  private final ClassSessionRepository sessions;
  private final AttendanceRecordRepository attendance;
  private final AssessmentRepository assessments;
  private final SubmissionRepository submissions;
  private final GradeRepository grades;
  private final MessageRepository messages;
  private final NotificationRepository notifications;
  private final CalendarEventRepository events;
  private final LearningResourceRepository resources;

  public FacultyService(
    UserRepository u,
    FacultyRepository f,
    StudentRepository st,
    CourseRepository c,
    EnrollmentRepository e,
    ClassSessionRepository s,
    AttendanceRecordRepository a,
    AssessmentRepository ass,
    SubmissionRepository sub,
    GradeRepository g,
    MessageRepository m,
    NotificationRepository n,
    CalendarEventRepository ev,
    LearningResourceRepository r
  ) {
    users = u;
    faculty = f;
    students = st;
    courses = c;
    enrollments = e;
    sessions = s;
    attendance = a;
    assessments = ass;
    submissions = sub;
    grades = g;
    messages = m;
    notifications = n;
    events = ev;
    resources = r;
  }

  public UserRepository users() {
    return users;
  }

  public FacultyRepository faculty() {
    return faculty;
  }

  public StudentRepository students() {
    return students;
  }

  public CourseRepository courses() {
    return courses;
  }

  public EnrollmentRepository enrollments() {
    return enrollments;
  }

  public ClassSessionRepository sessions() {
    return sessions;
  }

  public AttendanceRecordRepository attendance() {
    return attendance;
  }

  public AssessmentRepository assessments() {
    return assessments;
  }

  public SubmissionRepository submissions() {
    return submissions;
  }

  public GradeRepository grades() {
    return grades;
  }

  public MessageRepository messages() {
    return messages;
  }

  public NotificationRepository notifications() {
    return notifications;
  }

  public CalendarEventRepository events() {
    return events;
  }

  public LearningResourceRepository resources() {
    return resources;
  }

  public User user(String name) {
    return users.findByIdentifier(name).orElseThrow();
  }

  public Faculty faculty(String name) {
    return faculty
      .findByUserId(user(name).id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
  }

  public Course course(String name, Long id) {
    var c = courses
      .findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (
      !c.faculty.id.equals(faculty(name).id)
    ) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return c;
  }

  public List<Map<String, Object>> courses(String name) {
    return courses
      .findByFacultyId(faculty(name).id)
      .stream()
      .map(c ->
        Map.<String, Object>of(
          "id",
          c.id,
          "code",
          c.code,
          "title",
          c.title,
          "section",
          c.section,
          "description",
          Objects.toString(c.description, ""),
          "students",
          enrollments.findByCourseId(c.id).size()
        )
      )
      .toList();
  }

  public List<Map<String, Object>> roster(String name) {
    return courses
      .findByFacultyId(faculty(name).id)
      .stream()
      .flatMap(c ->
        enrollments
          .findByCourseId(c.id)
          .stream()
          .map(e ->
            Map.<String, Object>of(
              "id",
              e.student.id,
              "userId",
              e.student.user.id,
              "name",
              e.student.user.name,
              "enrollmentNumber",
              e.student.enrollmentNumber,
              "program",
              e.student.program,
              "courseId",
              c.id,
              "course",
              c.title
            )
          )
      )
      .toList();
  }

  public String status(ClassSession s) {
    var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    return s.cancelled
      ? "Cancelled"
      : now.isBefore(s.startsAt)
        ? "Upcoming"
        : now.isAfter(s.endsAt)
          ? "Completed"
          : "Ongoing";
  }

  public List<Map<String, Object>> schedule(String name) {
    return sessions
      .findByCourseFacultyId(faculty(name).id)
      .stream()
      .sorted(Comparator.comparing(s -> s.startsAt))
      .map(s ->
        Map.<String, Object>of(
          "id",
          s.id,
          "courseId",
          s.course.id,
          "course",
          s.course.title + " · " + s.course.section,
          "startsAt",
          s.startsAt,
          "endsAt",
          s.endsAt,
          "room",
          s.room,
          "status",
          status(s)
        )
      )
      .toList();
  }

  public Map<String, Object> dashboard(String name) {
    var id = faculty(name).id;
    var records = attendance.findByClassSessionCourseFacultyId(id);
    var completed = sessions
      .findByCourseFacultyId(id)
      .stream()
      .filter(s -> status(s).equals("Completed"))
      .toList();
    long expected = completed
      .stream()
      .mapToLong(s -> enrollments.findByCourseId(s.course.id).size())
      .sum();
    long present = records
      .stream()
      .filter(r ->
        completed.stream().anyMatch(s -> s.id.equals(r.classSession.id))
      )
      .count();
    var pending = submissions
      .findByAssessmentCourseFacultyId(id)
      .stream()
      .filter(s ->
        grades
          .findAll()
          .stream()
          .noneMatch(g -> g.submission.id.equals(s.id))
      )
      .count();
    return Map.of(
      "activeClasses",
      courses(name).size(),
      "totalStudents",
      roster(name)
        .stream()
        .map(s -> s.get("id"))
        .distinct()
        .count(),
      "attendancePercentage",
      expected == 0 ? 0 : Math.round((present * 100.0) / expected),
      "pendingReviews",
      pending,
      "actions",
      assessments
        .findByCourseFacultyId(id)
        .stream()
        .filter(a -> !a.published)
        .map(a ->
          Map.of(
            "title",
            "Review " + a.title,
            "course",
            a.course.section,
            "status",
            "Draft",
            "path",
            "/faculty/assessments"
          )
        )
        .toList()
    );
  }

  public List<Map<String, Object>> calendar(String name) {
    var result = new ArrayList<Map<String, Object>>();
    events
      .findByFacultyId(faculty(name).id)
      .forEach(e ->
        result.add(
          Map.of("id", "event-" + e.id, "title", e.title, "date", e.eventDate)
        )
      );
    sessions
      .findByCourseFacultyId(faculty(name).id)
      .forEach(s ->
        result.add(
          Map.of(
            "id",
            "class-" + s.id,
            "title",
            s.course.title + " · " + s.room,
            "date",
            s.startsAt.toLocalDate()
          )
        )
      );
    assessments
      .findByCourseFacultyId(faculty(name).id)
      .forEach(a ->
        result.add(
          Map.of(
            "id",
            "assessment-" + a.id,
            "title",
            a.title + " deadline",
            "date",
            a.deadline.toLocalDate()
          )
        )
      );
    return result;
  }

  public boolean canMessage(String name, Long receiver) {
    return roster(name)
      .stream()
      .anyMatch(s -> s.get("userId").equals(receiver));
  }

  public List<Map<String, Object>> messages(String name) {
    var u = user(name);
    return messages
      .findBySenderIdOrReceiverIdOrderBySentAtAsc(u.id, u.id)
      .stream()
      .map(m ->
        Map.<String, Object>of(
          "id",
          m.id,
          "senderId",
          m.sender.id,
          "sender",
          m.sender.name,
          "receiverId",
          m.receiver.id,
          "receiver",
          m.receiver.name,
          "body",
          m.body,
          "sentAt",
          m.sentAt
        )
      )
      .toList();
  }

  public List<Map<String, Object>> assessments(String name) {
    return assessments
      .findByCourseFacultyId(faculty(name).id)
      .stream()
      .map(a ->
        Map.<String, Object>of(
          "id",
          a.id,
          "courseId",
          a.course.id,
          "course",
          a.course.title,
          "title",
          a.title,
          "kind",
          a.kind,
          "deadline",
          a.deadline,
          "published",
          a.published
        )
      )
      .toList();
  }

  public static String hash(String token) {
    try {
      return HexFormat.of().formatHex(
        java.security.MessageDigest.getInstance("SHA-256").digest(
          token.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        )
      );
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
