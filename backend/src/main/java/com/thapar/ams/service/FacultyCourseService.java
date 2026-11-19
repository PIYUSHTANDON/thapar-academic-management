package com.thapar.ams.service;

import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.entity.ClassSession;
import com.thapar.ams.entity.LearningResource;
import com.thapar.ams.entity.Notification;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyCourseService {
  private final FacultyService s;

  public FacultyCourseService(FacultyService s) {
    this.s = s;
  }

  public List<FacultyCourse> courses(String name) {
    return s.courses()
      .findByFacultyId(s.faculty(name).id)
      .stream()
      .map(c -> new FacultyCourse(
        c.id,
        c.code,
        c.title,
        c.section,
        Objects.toString(c.description, ""),
        s.enrollments().findByCourseId(c.id).size()
      ))
      .toList();
  }

  public List<RosterEntry> roster(String name) {
    return s.courses()
      .findByFacultyId(s.faculty(name).id)
      .stream()
      .flatMap(c ->
        s.enrollments()
          .findByCourseId(c.id)
          .stream()
          .map(e -> new RosterEntry(
            e.student.id,
            e.student.user.id,
            e.student.user.name,
            e.student.enrollmentNumber,
            e.student.program,
            c.id,
            c.title
          ))
      )
      .toList();
  }

  public String status(ClassSession session) {
    var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    return session.cancelled
      ? "Cancelled"
      : now.isBefore(session.startsAt)
        ? "Upcoming"
        : now.isAfter(session.endsAt)
          ? "Completed"
          : "Ongoing";
  }

  public List<ClassSessionItem> schedule(String name) {
    return s.sessions()
      .findByCourseFacultyId(s.faculty(name).id)
      .stream()
      .sorted(Comparator.comparing(session -> session.startsAt))
      .map(session -> new ClassSessionItem(
        session.id,
        session.course.id,
        session.course.title + " · " + session.course.section,
        session.startsAt,
        session.endsAt,
        session.room,
        status(session)
      ))
      .toList();
  }

  public void updateCourse(String name, Long id, String title, String description) {
    var course = s.course(name, id);
    course.title = title;
    course.description = description;
    s.courses().save(course);
  }

  public List<ResourceItem> resources(String name, Long courseId) {
    s.course(name, courseId);
    return s.resources().findByCourseId(courseId).stream()
      .map(r -> new ResourceItem(r.id, r.title, r.kind))
      .toList();
  }

  public void upload(String name, Long courseId, MultipartFile file) throws Exception {
    var resource = new LearningResource();
    resource.course = s.course(name, courseId);
    resource.title = Objects.toString(file.getOriginalFilename(), "Resource");
    if (resource.title.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    resource.kind = "File";
    resource.content = file.getBytes();
    resource.contentType = "application/octet-stream";
    s.resources().save(resource);
  }

  public LearningResource download(String name, Long id) {
    var resource = s.resources().findById(id).orElseThrow();
    s.course(name, resource.course.id);
    return resource;
  }

  public void announce(String name, Long courseId, String title, String description) {
    var course = s.course(name, courseId);
    var resource = new LearningResource();
    resource.course = course;
    resource.title = title;
    resource.kind = "Announcement";
    resource.description = description;
    s.resources().save(resource);
    s.enrollments().findByCourseId(courseId).forEach(enrollment -> {
      var notification = new Notification();
      notification.user = enrollment.student.user;
      notification.title = course.code + ": " + title;
      s.notifications().save(notification);
    });
  }

  public void updateSession(String name, Long id, LocalDateTime startsAt, LocalDateTime endsAt, String room, boolean cancelled) {
    var session = s.sessions().findById(id).orElseThrow();
    s.course(name, session.course.id);
    if (!endsAt.isAfter(startsAt))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class end must be after its start");
    session.startsAt = startsAt;
    session.endsAt = endsAt;
    session.room = room;
    session.cancelled = cancelled;
    session.tokenHash = null;
    session.tokenExpiresAt = null;
    s.sessions().save(session);
  }
}
