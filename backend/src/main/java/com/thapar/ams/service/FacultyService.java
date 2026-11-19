package com.thapar.ams.service;

import com.thapar.ams.entity.User;
import com.thapar.ams.entity.Faculty;
import com.thapar.ams.entity.Course;
import com.thapar.ams.repository.*;
import java.util.HexFormat;
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
