package com.thapar.ams.service;

import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.StudentDashboard;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentOverviewService {
  private final FacultyService s;
  private final StudentService access;
  private final StudentCourseService courses;
  private final StudentAttendanceService attendance;
  private final StudentAssessmentService assessments;

  public StudentOverviewService(
    FacultyService s,
    StudentService access,
    StudentCourseService courses,
    StudentAttendanceService attendance,
    StudentAssessmentService assessments
  ) {
    this.s = s;
    this.access = access;
    this.courses = courses;
    this.attendance = attendance;
    this.assessments = assessments;
  }

  public List<CalendarEvent> calendar(String name) {
    var list = new ArrayList<CalendarEvent>();
    for (var v : courses.sessions(name))
      list.add(new CalendarEvent("class-" + v.id, v.course.title + " - " + v.room, v.startsAt.toLocalDate(), "Class"));
    for (var a : assessments.available(name))
      list.add(new CalendarEvent("assessment-" + a.id, a.title + " deadline", a.deadline.toLocalDate(), a.kind));
    var ids = access.enrolled(name).stream().map(c -> c.faculty.id).distinct().toList();
    for (var e : s.events().findByFacultyIdIn(ids))
      list.add(new CalendarEvent("event-" + e.id, e.title, e.eventDate, "Event"));
    return list;
  }

  public StudentDashboard dashboard(String name) {
    var st = access.student(name);
    long pending = assessments.available(name)
      .stream()
      .filter(a ->
        "Assignment".equals(a.kind) &&
        a.deadline.isAfter(StudentService.now()) &&
        s.submissions().findByAssessmentIdAndStudentId(a.id, st.id).isEmpty()
      )
      .count();
    return new StudentDashboard(
      attendance.attendance(name).percentage(),
      st.officialCgpa,
      pending,
      access.enrolled(name).size()
    );
  }
}
