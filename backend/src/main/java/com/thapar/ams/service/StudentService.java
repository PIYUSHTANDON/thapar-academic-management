package com.thapar.ams.service;

import com.thapar.ams.entity.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentService {

  private final FacultyService s;

  public StudentService(FacultyService s) {
    this.s = s;
  }

  public Student student(String name) {
    return s
      .students()
      .findByUserId(s.user(name).id)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Student profile is unavailable"
        )
      );
  }

  public List<Course> enrolled(String name) {
    return s
      .enrollments()
      .findByStudentIdAndActiveTrue(student(name).id)
      .stream()
      .map(e -> e.course)
      .toList();
  }

  public List<Long> courseIds(String name) {
    return enrolled(name)
      .stream()
      .map(c -> c.id)
      .toList();
  }

  public Course course(String name, Long id) {
    return enrolled(name)
      .stream()
      .filter(c -> c.id.equals(id))
      .findFirst()
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You are not enrolled in this course"
        )
      );
  }

  public static Map<String, Object> dto(Object... pairs) {
    var result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) result.put(
      (String) pairs[i],
      pairs[i + 1]
    );
    return result;
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
  }

  public Map<String, Object> profile(String name) {
    var st = student(name);
    return dto(
      "id",
      st.id,
      "name",
      st.user.name,
      "identifier",
      st.user.identifier,
      "enrollmentNumber",
      st.enrollmentNumber,
      "program",
      st.program,
      "academicYear",
      st.academicYear,
      "semester",
      st.semester,
      "officialCgpa",
      st.officialCgpa,
      "notificationsEnabled",
      st.user.notificationsEnabled,
      "synthetic",
      st.user.localOnly ||
        Objects.toString(st.program, "").toLowerCase().contains("synthetic")
    );
  }

  public List<Map<String, Object>> courses(String name) {
    return enrolled(name)
      .stream()
      .map(c ->
        dto(
          "id",
          c.id,
          "code",
          c.code,
          "title",
          c.title,
          "section",
          c.section,
          "description",
          c.description,
          "faculty",
          c.faculty.user.name,
          "facultyUserId",
          c.faculty.user.id
        )
      )
      .toList();
  }

  public List<ClassSession> sessions(String name) {
    return s
      .sessions()
      .findByCourseIdIn(courseIds(name))
      .stream()
      .sorted(Comparator.comparing(v -> v.startsAt))
      .toList();
  }

  public List<Map<String, Object>> schedule(String name) {
    return sessions(name)
      .stream()
      .map(v ->
        dto(
          "id",
          v.id,
          "courseId",
          v.course.id,
          "course",
          v.course.title + " - " + v.course.section,
          "room",
          v.room,
          "startsAt",
          v.startsAt,
          "endsAt",
          v.endsAt,
          "status",
          s.status(v)
        )
      )
      .toList();
  }

  public Map<String, Object> attendance(String name) {
    var st = student(name);
    var records = s.attendance().findByStudentId(st.id);
    var summary = new ArrayList<Map<String, Object>>();
    var details = new ArrayList<Map<String, Object>>();
    long total = 0,
      present = 0;
    for (var c : enrolled(name)) {
      var eligible = sessions(name)
        .stream()
        .filter(
          v -> v.course.id.equals(c.id) && s.status(v).equals("Completed")
        )
        .toList();
      long attended = eligible
        .stream()
        .filter(v ->
          records.stream().anyMatch(r -> r.classSession.id.equals(v.id))
        )
        .count();
      total += eligible.size();
      present += attended;
      summary.add(
        dto(
          "courseId",
          c.id,
          "course",
          c.title,
          "section",
          c.section,
          "attended",
          attended,
          "total",
          eligible.size(),
          "missed",
          eligible.size() - attended,
          "percentage",
          eligible.isEmpty()
            ? null
            : Math.round((attended * 100.0) / eligible.size())
        )
      );
      for (var v : sessions(name)
        .stream()
        .filter(
          v ->
            v.course.id.equals(c.id) &&
            (s.status(v).equals("Completed") ||
              records.stream().anyMatch(r -> r.classSession.id.equals(v.id)))
        )
        .toList())
        details.add(
          dto(
            "sessionId",
            v.id,
            "course",
            c.title,
            "startsAt",
            v.startsAt,
            "room",
            v.room,
            "status",
            records.stream().anyMatch(r -> r.classSession.id.equals(v.id))
              ? "Attended"
              : "Missed"
          )
        );
    }
    return dto(
      "percentage",
      total == 0 ? null : Math.round((present * 100.0) / total),
      "attended",
      present,
      "total",
      total,
      "courses",
      summary,
      "records",
      details
    );
  }

  public List<Assessment> available(String name) {
    return s.assessments().findByCourseIdIn(courseIds(name));
  }

  public Map<String, Object> assessment(String name, Assessment a) {
    var st = student(name);
    var submission = s
      .submissions()
      .findByAssessmentIdAndStudentId(a.id, st.id)
      .orElse(null);
    var grade =
      submission == null
        ? null
        : s.grades().findBySubmissionId(submission.id).orElse(null);
    boolean released = grade != null && grade.published && a.published;
    String status =
      submission != null
        ? released
          ? "Graded"
          : "Submitted"
        : now().isAfter(a.deadline)
          ? "Overdue"
          : now().plusDays(2).isAfter(a.deadline)
            ? "Due Soon"
            : "Pending";
    return dto(
      "id",
      a.id,
      "title",
      a.title,
      "courseId",
      a.course.id,
      "course",
      a.course.title,
      "kind",
      a.kind,
      "deadline",
      a.deadline,
      "status",
      status,
      "submissionId",
      submission == null ? null : submission.id,
      "submittedAt",
      submission == null ? null : submission.submittedAt,
      "fileName",
      submission == null ? null : submission.fileName,
      "content",
      submission == null ? null : submission.content,
      "marks",
      released ? grade.marks : null,
      "feedback",
      released ? grade.feedback : null
    );
  }

  public List<Map<String, Object>> assessments(String name) {
    return available(name)
      .stream()
      .sorted(Comparator.comparing(a -> a.deadline))
      .map(a -> assessment(name, a))
      .toList();
  }

  public List<Map<String, Object>> results(String name) {
    return assessments(name)
      .stream()
      .filter(a -> a.get("marks") != null)
      .toList();
  }

  public List<Map<String, Object>> resources(String name, Long id) {
    course(name, id);
    return s
      .resources()
      .findByCourseId(id)
      .stream()
      .map(this::resource)
      .toList();
  }

  Map<String, Object> resource(LearningResource r) {
    return dto(
      "id",
      r.id,
      "title",
      r.title,
      "kind",
      r.kind,
      "description",
      r.description,
      "publishedAt",
      r.publishedAt,
      "courseId",
      r.course.id,
      "course",
      r.course.title,
      "faculty",
      r.course.faculty.user.name
    );
  }

  public List<Map<String, Object>> announcements(String name) {
    return s
      .resources()
      .findByCourseIdIn(courseIds(name))
      .stream()
      .filter(r -> "Announcement".equals(r.kind))
      .sorted(
        Comparator.comparing(
          (LearningResource r) -> r.publishedAt,
          Comparator.nullsLast(Comparator.reverseOrder())
        )
      )
      .map(this::resource)
      .toList();
  }

  public List<Map<String, Object>> calendar(String name) {
    var list = new ArrayList<Map<String, Object>>();
    for (var v : sessions(name))
      list.add(
        dto(
          "id",
          "class-" + v.id,
          "title",
          v.course.title + " - " + v.room,
          "date",
          v.startsAt.toLocalDate(),
          "kind",
          "Class"
        )
      );
    for (var a : available(name))
      list.add(
        dto(
          "id",
          "assessment-" + a.id,
          "title",
          a.title + " deadline",
          "date",
          a.deadline.toLocalDate(),
          "kind",
          a.kind
        )
      );
    var ids = enrolled(name)
      .stream()
      .map(c -> c.faculty.id)
      .distinct()
      .toList();
    for (var e : s.events().findByFacultyIdIn(ids))
      list.add(
        dto(
          "id",
          "event-" + e.id,
          "title",
          e.title,
          "date",
          e.eventDate,
          "kind",
          "Event"
        )
      );
    return list;
  }

  public List<Map<String, Object>> teachers(String name) {
    return enrolled(name)
      .stream()
      .map(c -> c.faculty.user)
      .distinct()
      .map(u -> dto("id", u.id, "name", u.name))
      .toList();
  }

  public boolean canMessage(String name, Long id) {
    return teachers(name)
      .stream()
      .anyMatch(t -> t.get("id").equals(id));
  }

  public List<Map<String, Object>> messages(String name) {
    var u = student(name).user;
    return s
      .messages()
      .findBySenderIdOrReceiverIdOrderBySentAtAsc(u.id, u.id)
      .stream()
      .filter(m ->
        canMessage(name, m.sender.id.equals(u.id) ? m.receiver.id : m.sender.id)
      )
      .map(m ->
        dto(
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

  public Map<String, Object> dashboard(String name) {
    var st = student(name);
    long pending = available(name)
      .stream()
      .filter(
        a ->
          "Assignment".equals(a.kind) &&
          a.deadline.isAfter(now()) &&
          s.submissions().findByAssessmentIdAndStudentId(a.id, st.id).isEmpty()
      )
      .count();
    return dto(
      "overallAttendance",
      attendance(name).get("percentage"),
      "currentCgpa",
      st.officialCgpa,
      "pendingAssignments",
      pending,
      "registeredCourses",
      enrolled(name).size()
    );
  }
}
