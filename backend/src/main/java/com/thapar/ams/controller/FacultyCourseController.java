package com.thapar.ams.controller;

import com.thapar.ams.contract.CourseContracts.AnnouncementRequest;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.contract.CourseContracts.UpdateCourseRequest;
import com.thapar.ams.contract.CourseContracts.UpdateScheduleRequest;
import com.thapar.ams.service.FacultyCourseService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Transactional
@RequestMapping("/api/v1/faculty")
public class FacultyCourseController {
  private final FacultyCourseService courseService;

  public FacultyCourseController(FacultyCourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping("/courses")
  List<FacultyCourse> courses(Principal p) {
    return courseService.courses(p.getName());
  }

  @GetMapping("/students")
  List<RosterEntry> students(Principal p) {
    return courseService.roster(p.getName());
  }

  @GetMapping("/schedule")
  List<ClassSessionItem> schedule(Principal p) {
    return courseService.schedule(p.getName());
  }

  @PutMapping("/courses/{id}")
  void course(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody UpdateCourseRequest b
  ) {
    courseService.updateCourse(p.getName(), id, b.title(), b.description());
  }

  @GetMapping("/courses/{id}/resources")
  List<ResourceItem> resources(Principal p, @PathVariable Long id) {
    return courseService.resources(p.getName(), id);
  }

  @PostMapping("/courses/{id}/resources")
  void upload(
    Principal p,
    @PathVariable Long id,
    @RequestParam MultipartFile file
  ) throws Exception {
    courseService.upload(p.getName(), id, file);
  }

  @GetMapping("/resources/{id}")
  ResponseEntity<byte[]> download(Principal p, @PathVariable Long id) {
    var r = courseService.download(p.getName(), id);
    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=resource-" + id)
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(r.content);
  }

  @PostMapping("/courses/{id}/announcements")
  void announce(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody AnnouncementRequest b
  ) {
    courseService.announce(p.getName(), id, b.title(), b.description());
  }

  @PutMapping("/schedule/{id}")
  void updateSession(
    Principal p,
    @PathVariable Long id,
    @Valid @RequestBody UpdateScheduleRequest b
  ) {
    courseService.updateSession(p.getName(), id, b.startsAt(), b.endsAt(), b.room(), b.cancelled());
  }
}
