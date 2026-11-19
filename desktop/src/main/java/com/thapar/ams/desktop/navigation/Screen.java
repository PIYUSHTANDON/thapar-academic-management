package com.thapar.ams.desktop.navigation;

import com.thapar.ams.contract.AuthContracts.UserRole;
import java.util.Arrays;
import java.util.List;

public enum Screen {
  FACULTY_DASHBOARD("Dashboard", UserRole.FACULTY),
  FACULTY_MESSAGES("Messages", UserRole.FACULTY),
  FACULTY_ATTENDANCE("Attendance", UserRole.FACULTY),
  FACULTY_COURSES("Courses & LMS", UserRole.FACULTY),
  FACULTY_ASSESSMENTS("Assessments", UserRole.FACULTY),
  FACULTY_CALENDAR("Calendar", UserRole.FACULTY),
  FACULTY_STUDENTS("Student Roster", UserRole.FACULTY),
  FACULTY_SETTINGS("Settings", UserRole.FACULTY),
  FACULTY_REPORTS("Attendance Reports", UserRole.FACULTY),
  STUDENT_DASHBOARD("Dashboard", UserRole.STUDENT),
  STUDENT_MESSAGES("Messages", UserRole.STUDENT),
  STUDENT_ATTENDANCE("Attendance", UserRole.STUDENT),
  STUDENT_COURSES("Courses & LMS", UserRole.STUDENT),
  STUDENT_ASSESSMENTS("Assessments", UserRole.STUDENT),
  STUDENT_CALENDAR("Calendar", UserRole.STUDENT),
  STUDENT_RESULTS("Results", UserRole.STUDENT),
  STUDENT_SETTINGS("Settings", UserRole.STUDENT);

  private final String title;
  private final UserRole role;

  Screen(String title, UserRole role) {
    this.title = title;
    this.role = role;
  }

  public String title() { return title; }
  public UserRole role() { return role; }

  public static Screen home(UserRole role) {
    return role == UserRole.FACULTY ? FACULTY_DASHBOARD : STUDENT_DASHBOARD;
  }

  public static List<Screen> navigation(UserRole role) {
    return Arrays.stream(values())
      .filter(screen -> screen.role == role && screen != FACULTY_REPORTS)
      .toList();
  }
}
