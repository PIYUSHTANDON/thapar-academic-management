package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import com.thapar.ams.contract.AttendanceContracts.ClaimAttendanceRequest;
import com.thapar.ams.contract.AttendanceContracts.FacultyAttendanceRecord;
import com.thapar.ams.contract.AttendanceContracts.MessageResponse;
import com.thapar.ams.contract.AttendanceContracts.OpenSessionRequest;
import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceSummary;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import java.util.List;

public final class AttendanceApi {
  private final ApiClient api;

  public AttendanceApi(ApiClient api) { this.api = api; }

  public List<ClassSessionItem> sessions() {
    return api.get("faculty/schedule", new TypeReference<>() { });
  }

  public List<FacultyAttendanceRecord> facultyRecords() {
    return api.get("faculty/attendance", new TypeReference<>() { });
  }

  public AttendanceToken open(long sessionId) {
    return api.post("faculty/attendance/sessions", new OpenSessionRequest(sessionId), AttendanceToken.class);
  }

  public StudentAttendanceSummary studentSummary() {
    return api.get("student/attendance", StudentAttendanceSummary.class);
  }

  public MessageResponse claim(String token) {
    return api.post("student/attendance/claim", new ClaimAttendanceRequest(token), MessageResponse.class);
  }
}
