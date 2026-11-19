package com.thapar.ams.service;

import com.thapar.ams.entity.Course;
import com.thapar.ams.entity.Student;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

  public static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
  }
}
