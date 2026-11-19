package com.thapar.ams.service;

import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.FacultyAction;
import com.thapar.ams.contract.OverviewContracts.FacultyDashboard;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FacultyOverviewService {
  private final FacultyService s;
  private final FacultyCourseService course;

  public FacultyOverviewService(FacultyService s, FacultyCourseService course) {
    this.s = s;
    this.course = course;
  }

  public FacultyDashboard dashboard(String name) {
    var id = s.faculty(name).id;
    var records = s.attendance().findByClassSessionCourseFacultyId(id);
    var completed = s.sessions()
      .findByCourseFacultyId(id)
      .stream()
      .filter(session -> course.status(session).equals("Completed"))
      .toList();
    long expected = completed
      .stream()
      .mapToLong(session -> s.enrollments().findByCourseId(session.course.id).size())
      .sum();
    long present = records
      .stream()
      .filter(r -> completed.stream().anyMatch(session -> session.id.equals(r.classSession.id)))
      .count();
    long pending = s.submissions()
      .findByAssessmentCourseFacultyId(id)
      .stream()
      .filter(submission ->
        s.grades().findAll().stream().noneMatch(g -> g.submission.id.equals(submission.id))
      )
      .count();
    var actions = s.assessments()
      .findByCourseFacultyId(id)
      .stream()
      .filter(a -> !a.published)
      .map(a -> new FacultyAction("Review " + a.title, a.course.section, "Draft", "/faculty/assessments"))
      .toList();
    return new FacultyDashboard(
      course.courses(name).size(),
      course.roster(name).stream().map(entry -> entry.id()).distinct().count(),
      expected == 0 ? 0 : Math.round((present * 100.0) / expected),
      pending,
      actions
    );
  }

  public List<CalendarEvent> calendar(String name) {
    var result = new ArrayList<CalendarEvent>();
    s.events()
      .findByFacultyId(s.faculty(name).id)
      .forEach(e -> result.add(new CalendarEvent("event-" + e.id, e.title, e.eventDate, null)));
    s.sessions()
      .findByCourseFacultyId(s.faculty(name).id)
      .forEach(session -> result.add(new CalendarEvent(
        "class-" + session.id,
        session.course.title + " · " + session.room,
        session.startsAt.toLocalDate(),
        null
      )));
    s.assessments()
      .findByCourseFacultyId(s.faculty(name).id)
      .forEach(a -> result.add(new CalendarEvent(
        "assessment-" + a.id,
        a.title + " deadline",
        a.deadline.toLocalDate(),
        null
      )));
    return result;
  }

  public void createEvent(String name, String title, LocalDate eventDate) {
    var event = new com.thapar.ams.entity.CalendarEvent();
    event.faculty = s.faculty(name);
    event.title = title;
    event.eventDate = eventDate;
    s.events().save(event);
  }
}
