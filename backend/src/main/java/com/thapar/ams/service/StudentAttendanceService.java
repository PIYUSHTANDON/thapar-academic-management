package com.thapar.ams.service;

import com.thapar.ams.contract.AttendanceContracts.CourseAttendanceSummary;
import com.thapar.ams.contract.AttendanceContracts.MessageResponse;
import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceRecord;
import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceSummary;
import com.thapar.ams.entity.AttendanceRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentAttendanceService {
  private final FacultyService s;
  private final StudentService access;
  private final StudentCourseService courses;
  private final FacultyCourseService facultyCourses;

  public StudentAttendanceService(
    FacultyService s,
    StudentService access,
    StudentCourseService courses,
    FacultyCourseService facultyCourses
  ) {
    this.s = s;
    this.access = access;
    this.courses = courses;
    this.facultyCourses = facultyCourses;
  }

  public StudentAttendanceSummary attendance(String name) {
    var st = access.student(name);
    var records = s.attendance().findByStudentId(st.id);
    var summary = new ArrayList<CourseAttendanceSummary>();
    var details = new ArrayList<StudentAttendanceRecord>();
    long total = 0, present = 0;
    for (var c : access.enrolled(name)) {
      var eligible = courses.sessions(name)
        .stream()
        .filter(v -> v.course.id.equals(c.id) && facultyCourses.status(v).equals("Completed"))
        .toList();
      long attended = eligible
        .stream()
        .filter(v -> records.stream().anyMatch(r -> r.classSession.id.equals(v.id)))
        .count();
      total += eligible.size();
      present += attended;
      summary.add(new CourseAttendanceSummary(
        c.id,
        c.title,
        c.section,
        attended,
        eligible.size(),
        eligible.size() - attended,
        eligible.isEmpty() ? null : Math.round((attended * 100.0) / eligible.size())
      ));
      for (var v : courses.sessions(name)
        .stream()
        .filter(v ->
          v.course.id.equals(c.id) &&
          (facultyCourses.status(v).equals("Completed") ||
            records.stream().anyMatch(r -> r.classSession.id.equals(v.id)))
        )
        .toList())
        details.add(new StudentAttendanceRecord(
          v.id,
          c.title,
          v.startsAt,
          v.room,
          records.stream().anyMatch(r -> r.classSession.id.equals(v.id)) ? "Attended" : "Missed"
        ));
    }
    return new StudentAttendanceSummary(
      total == 0 ? null : Math.round((present * 100.0) / total),
      present,
      total,
      summary,
      details
    );
  }

  public MessageResponse claim(String name, String token) {
    var student = access.student(name);
    var session = s.sessions().findByTokenHash(FacultyService.hash(token))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance token"));
    access.course(name, session.course.id);
    if (session.cancelled || session.endsAt.isBefore(StudentService.now()) ||
        session.tokenExpiresAt.isBefore(StudentService.now()) ||
        !s.enrollments().existsByStudentIdAndCourseId(student.id, session.course.id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Expired token or student is not enrolled");
    if (s.attendance().existsByStudentIdAndClassSessionId(student.id, session.id))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded");
    var record = new AttendanceRecord();
    record.student = student;
    record.classSession = session;
    record.recordedAt = LocalDateTime.now();
    s.attendance().save(record);
    return new MessageResponse("Attendance recorded for authenticated student");
  }
}
