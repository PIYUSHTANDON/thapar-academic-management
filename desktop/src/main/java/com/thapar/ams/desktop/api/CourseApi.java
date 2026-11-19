package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thapar.ams.contract.CourseContracts.AnnouncementRequest;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.contract.CourseContracts.StudentCourse;
import com.thapar.ams.contract.CourseContracts.UpdateCourseRequest;
import com.thapar.ams.contract.CourseContracts.UpdateScheduleRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CourseApi {
  private final ApiClient api;

  public CourseApi(ApiClient api) { this.api = api; }

  public List<FacultyCourse> facultyCourses() {
    return api.get("faculty/courses", new TypeReference<>() { });
  }

  public List<StudentCourse> studentCourses() {
    return api.get("student/courses", new TypeReference<>() { });
  }

  public List<RosterEntry> roster() {
    return api.get("faculty/students", new TypeReference<>() { });
  }

  public List<ClassSessionItem> facultySchedule() {
    return api.get("faculty/schedule", new TypeReference<>() { });
  }

  public List<ClassSessionItem> studentSchedule() {
    return api.get("student/schedule", new TypeReference<>() { });
  }

  public List<ResourceItem> facultyResources(long courseId) {
    return api.get("faculty/courses/" + courseId + "/resources", new TypeReference<>() { });
  }

  public List<ResourceItem> studentResources(long courseId) {
    return api.get("student/courses/" + courseId + "/resources", new TypeReference<>() { });
  }

  public void updateCourse(long courseId, String title, String description) {
    api.send("PUT", "faculty/courses/" + courseId, new UpdateCourseRequest(title, description));
  }

  public void upload(long courseId, Path file) throws IOException {
    if (file == null || !Files.isRegularFile(file)) throw new IllegalArgumentException("Choose a resource file.");
    if (file.getFileName().toString().length() > 255) throw new IllegalArgumentException("File name must be at most 255 characters.");
    if (Files.size(file) > 10 * 1024 * 1024) throw new IllegalArgumentException("Resource files must be at most 10 MB.");
    api.postMultipart("faculty/courses/" + courseId + "/resources", Map.of(), file);
  }

  public void announce(long courseId, String title, String description) {
    api.send("POST", "faculty/courses/" + courseId + "/announcements", new AnnouncementRequest(title, description));
  }

  public byte[] facultyFile(long resourceId) {
    return api.download("faculty/resources/" + resourceId);
  }

  public byte[] studentFile(long resourceId) {
    return api.download("student/resources/" + resourceId);
  }

  public void updateSession(long id, LocalDateTime start, LocalDateTime end, String room, boolean cancelled) {
    api.send("PUT", "faculty/schedule/" + id, new UpdateScheduleRequest(start, end, room, cancelled));
  }
}
