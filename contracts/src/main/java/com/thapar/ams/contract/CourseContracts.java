package com.thapar.ams.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class CourseContracts {
  private CourseContracts() { }

  public record FacultyCourse(
    long id,
    String code,
    String title,
    String section,
    String description,
    long students
  ) { }

  public record StudentCourse(
    long id,
    String code,
    String title,
    String section,
    String description,
    String faculty,
    long facultyUserId
  ) { }

  public record RosterEntry(
    long id,
    long userId,
    String name,
    String enrollmentNumber,
    String program,
    long courseId,
    String course
  ) { }

  public record ClassSessionItem(
    long id,
    long courseId,
    String course,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String room,
    String status
  ) { }

  public record ResourceItem(
    long id,
    String title,
    String kind,
    @JsonInclude(JsonInclude.Include.NON_NULL) String description,
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime publishedAt,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long courseId,
    @JsonInclude(JsonInclude.Include.NON_NULL) String course,
    @JsonInclude(JsonInclude.Include.NON_NULL) String faculty
  ) {
    public ResourceItem(long id, String title, String kind) {
      this(id, title, kind, null, null, null, null, null);
    }
  }

  public record UpdateCourseRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description
  ) { }

  public record AnnouncementRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 4000) String description
  ) { }

  public record UpdateScheduleRequest(
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt,
    @NotBlank @Size(max = 100) String room,
    boolean cancelled
  ) { }

  public record SearchResult(String label, String kind, String path) { }
}
