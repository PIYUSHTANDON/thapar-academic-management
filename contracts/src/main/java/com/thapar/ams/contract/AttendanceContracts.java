package com.thapar.ams.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class AttendanceContracts {
  private AttendanceContracts() { }

  public record FacultyAttendanceRecord(
    long id,
    String student,
    long sessionId,
    String course,
    LocalDateTime recordedAt
  ) { }

  public record OpenSessionRequest(@NotNull Long sessionId) { }

  public record AttendanceToken(
    String token,
    LocalDateTime expiresAt,
    long sessionId
  ) { }

  public record CourseAttendanceSummary(
    long courseId,
    String course,
    String section,
    long attended,
    long total,
    long missed,
    Long percentage
  ) { }

  public record StudentAttendanceRecord(
    long sessionId,
    String course,
    LocalDateTime startsAt,
    String room,
    String status
  ) { }

  public record StudentAttendanceSummary(
    Long percentage,
    long attended,
    long total,
    List<CourseAttendanceSummary> courses,
    List<StudentAttendanceRecord> records
  ) { }

  public record ClaimAttendanceRequest(@NotBlank String token) { }

  public record MessageResponse(String message) { }
}
