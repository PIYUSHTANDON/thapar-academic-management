package com.thapar.ams.controller;

import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.SearchResult;
import com.thapar.ams.contract.CourseContracts.StudentCourse;
import com.thapar.ams.service.StudentCourseService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentCourseController {
  private final StudentCourseService courseService;

  public StudentCourseController(StudentCourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping("/courses")
  List<StudentCourse> courses(Principal p) {
    return courseService.courses(p.getName());
  }

  @GetMapping("/schedule")
  List<ClassSessionItem> schedule(Principal p) {
    return courseService.schedule(p.getName());
  }

  @GetMapping("/announcements")
  List<ResourceItem> announcements(Principal p) {
    return courseService.announcements(p.getName());
  }

  @GetMapping("/courses/{id}/resources")
  List<ResourceItem> resources(Principal p, @PathVariable Long id) {
    return courseService.resources(p.getName(), id);
  }

  @GetMapping("/resources/{id}")
  ResponseEntity<byte[]> resource(Principal p, @PathVariable Long id) {
    var r = courseService.download(p.getName(), id);
    return AttachmentResponse.attachment(r.content, "resource-" + id);
  }

  @GetMapping("/search")
  List<SearchResult> search(Principal p, @RequestParam(defaultValue = "") String q) {
    return courseService.search(p.getName(), q);
  }
}
