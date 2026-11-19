package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AccountContracts.StudentProfile;
import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.CreateEventRequest;
import com.thapar.ams.contract.OverviewContracts.FacultyDashboard;
import com.thapar.ams.contract.OverviewContracts.StudentDashboard;
import java.time.LocalDate;
import java.util.List;

public final class WorkspaceApi {
  private final ApiClient api;

  public WorkspaceApi(ApiClient api) { this.api = api; }

  public FacultyData facultyDashboard() {
    return new FacultyData(
      api.get("faculty/dashboard", FacultyDashboard.class),
      api.get("faculty/schedule", new TypeReference<>() { }),
      facultyEvents(), notifications("faculty"));
  }

  public StudentData studentDashboard() {
    return new StudentData(
      api.get("student/dashboard", StudentDashboard.class),
      api.get("student/profile", StudentProfile.class),
      api.get("student/schedule", new TypeReference<>() { }),
      api.get("student/assessments", new TypeReference<>() { }),
      api.get("student/announcements", new TypeReference<>() { }),
      studentEvents(), notifications("student"));
  }

  public List<CalendarEvent> facultyEvents() {
    return api.get("faculty/calendar", new TypeReference<>() { });
  }

  public List<CalendarEvent> studentEvents() {
    return api.get("student/calendar", new TypeReference<>() { });
  }

  public List<NotificationItem> notifications(String role) {
    return api.get(role + "/notifications", new TypeReference<>() { });
  }

  public void markRead(String role, long id) {
    api.send("POST", role + "/notifications/" + id + "/read", null);
  }

  public void addEvent(String title, LocalDate eventDate) {
    api.send("POST", "faculty/calendar", new CreateEventRequest(title, eventDate));
  }

  public record FacultyData(FacultyDashboard summary, List<ClassSessionItem> schedule,
                            List<CalendarEvent> events, List<NotificationItem> notifications) { }
  public record StudentData(StudentDashboard summary, StudentProfile profile, List<ClassSessionItem> schedule,
                            List<StudentAssessment> assessments, List<ResourceItem> announcements,
                            List<CalendarEvent> events, List<NotificationItem> notifications) { }
}
