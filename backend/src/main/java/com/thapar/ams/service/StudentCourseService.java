package com.thapar.ams.service;

import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.SearchResult;
import com.thapar.ams.contract.CourseContracts.StudentCourse;
import com.thapar.ams.entity.ClassSession;
import com.thapar.ams.entity.LearningResource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentCourseService {
  private final FacultyService s;
  private final StudentService access;
  private final FacultyCourseService facultyCourses;

  public StudentCourseService(FacultyService s, StudentService access, FacultyCourseService facultyCourses) {
    this.s = s;
    this.access = access;
    this.facultyCourses = facultyCourses;
  }

  public List<StudentCourse> courses(String name) {
    return access.enrolled(name)
      .stream()
      .map(c -> new StudentCourse(
        c.id,
        c.code,
        c.title,
        c.section,
        c.description,
        c.faculty.user.name,
        c.faculty.user.id
      ))
      .toList();
  }

  public List<ClassSession> sessions(String name) {
    return s.sessions()
      .findByCourseIdIn(access.courseIds(name))
      .stream()
      .sorted(Comparator.comparing(v -> v.startsAt))
      .toList();
  }

  public List<ClassSessionItem> schedule(String name) {
    return sessions(name)
      .stream()
      .map(v -> new ClassSessionItem(
        v.id,
        v.course.id,
        v.course.title + " - " + v.course.section,
        v.startsAt,
        v.endsAt,
        v.room,
        facultyCourses.status(v)
      ))
      .toList();
  }

  public List<ResourceItem> resources(String name, Long id) {
    access.course(name, id);
    return s.resources().findByCourseId(id).stream().map(this::resource).toList();
  }

  ResourceItem resource(LearningResource r) {
    return new ResourceItem(
      r.id,
      r.title,
      r.kind,
      r.description,
      r.publishedAt,
      r.course.id,
      r.course.title,
      r.course.faculty.user.name
    );
  }

  public List<ResourceItem> announcements(String name) {
    return s.resources()
      .findByCourseIdIn(access.courseIds(name))
      .stream()
      .filter(r -> "Announcement".equals(r.kind))
      .sorted(Comparator.comparing((LearningResource r) -> r.publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
      .map(this::resource)
      .toList();
  }

  public LearningResource download(String name, Long id) {
    var resource = s.resources().findById(id).orElseThrow();
    access.course(name, resource.course.id);
    if (!"File".equals(resource.kind)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return resource;
  }

  public List<SearchResult> search(String name, String query) {
    if (query.length() > 128) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    String term = query.toLowerCase(Locale.ROOT).trim();
    if (term.isEmpty()) return List.of();
    var results = new ArrayList<SearchResult>();
    for (var course : courses(name))
      if ((course.title() + " " + course.code()).toLowerCase(Locale.ROOT).contains(term))
        results.add(new SearchResult(course.title(), "Course", "/student/courses"));
    for (var resource : s.resources().findByCourseIdIn(access.courseIds(name)))
      if ((resource.title + " " + Objects.toString(resource.description, "")).toLowerCase(Locale.ROOT).contains(term))
        results.add(new SearchResult(resource.title, resource.kind, "/student/courses"));
    return results.stream().limit(12).toList();
  }
}
