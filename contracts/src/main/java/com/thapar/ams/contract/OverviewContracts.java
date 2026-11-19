package com.thapar.ams.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class OverviewContracts {
  private OverviewContracts() { }

  public record FacultyDashboard(
    long activeClasses,
    long totalStudents,
    long attendancePercentage,
    long pendingReviews,
    List<FacultyAction> actions
  ) { }

  public record FacultyAction(
    String title,
    String course,
    String status,
    String path
  ) { }

  public record StudentDashboard(
    Long overallAttendance,
    BigDecimal currentCgpa,
    long pendingAssignments,
    long registeredCourses
  ) { }

  public record CalendarEvent(
    String id,
    String title,
    LocalDate date,
    @JsonInclude(JsonInclude.Include.NON_NULL) String kind
  ) { }

  public record CreateEventRequest(
    @NotBlank @Size(max = 255) String title,
    @NotNull LocalDate eventDate
  ) { }
}
