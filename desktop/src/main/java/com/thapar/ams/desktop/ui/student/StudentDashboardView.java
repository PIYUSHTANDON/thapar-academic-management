package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.AccountContracts.StudentProfile;
import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.api.WorkspaceApi.StudentData;
import com.thapar.ams.desktop.navigation.Screen;
import com.thapar.ams.desktop.ui.CalendarWidget;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.DashboardUi;
import com.thapar.ams.desktop.ui.DashboardUi.Stat;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class StudentDashboardView extends ScrollPane {
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private final WorkspaceApi api;
  private final TaskRunner tasks;
  private final UserResponse user;
  private final Consumer<Screen> navigate;
  private final Label message = new Label();
  private final VBox page = new VBox(18);

  public StudentDashboardView(WorkspaceApi api, TaskRunner tasks, UserResponse user,
                              Consumer<Screen> navigate) {
    this.api = api;
    this.tasks = tasks;
    this.user = user;
    this.navigate = navigate;
    message.getStyleClass().add("feedback");
    page.getStyleClass().add("dashboard-page");
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load();
  }

  private void load() {
    message.setText("Loading your student workspace…");
    tasks.run(api::studentDashboard, data -> {
      render(data);
      message.setText("");
    }, error -> {
      message.setText(error.getMessage());
      var retry = new Button("Retry loading");
      retry.getStyleClass().add("secondary-button");
      retry.setOnAction(e -> load());
      page.getChildren().setAll(message, retry);
    });
  }

  private void render(StudentData data) {
    var summary = data.summary();
    String attendance = summary.overallAttendance() == null ? "N/A" :
      summary.overallAttendance() + "%";
    String cgpa = summary.currentCgpa() == null ? "N/A" : summary.currentCgpa().toString();
    var hero = DashboardUi.hero(user, "Track your academics, classes, assignments, and progress in one place.",
      List.of(new Stat("Overall Attendance", attendance), new Stat("Current CGPA", cgpa),
        new Stat("Pending Assignments", Long.toString(summary.pendingAssignments())),
        new Stat("Registered Courses", Long.toString(summary.registeredCourses()))),
      data.notifications(), api, tasks, message);
    var refresh = new Button("Refresh dashboard");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    var quick = DashboardUi.actions(
      DashboardUi.action("View Timetable", Screen.STUDENT_CALENDAR, navigate),
      DashboardUi.action("Submit Assignment", Screen.STUDENT_ASSESSMENTS, navigate),
      DashboardUi.action("Check Results", Screen.STUDENT_RESULTS, navigate));
    var timetable = DashboardUi.panel("Today's Schedule", DashboardUi.today(data.schedule(),
      "No classes scheduled today."), link("View full timetable →", Screen.STUDENT_CALENDAR));
    var deadlines = DashboardUi.panel("Upcoming Deadlines", deadlineRows(data.assessments()),
      link("View all assessments →", Screen.STUDENT_ASSESSMENTS));
    timetable.setMinWidth(0);
    deadlines.setMinWidth(0);
    var middle = new FlowPane(14, 14, timetable, deadlines);
    middle.setMinWidth(0);
    middle.setMaxWidth(Double.MAX_VALUE);
    middle.widthProperty().addListener((changed, before, after) -> {
      double width = after.doubleValue();
      if (width <= 0) return;
      double cardWidth = width >= 600 ? (width - 16) / 2 : width;
      timetable.setPrefWidth(cardWidth);
      deadlines.setPrefWidth(cardWidth);
    });
    var left = new VBox(18, DashboardUi.panel("Quick Actions", quick), middle,
      DashboardUi.panel("Recent Announcements", announcementRows(data.announcements()),
        link("View all announcements →", Screen.STUDENT_COURSES)));
    var calendar = new CalendarWidget();
    calendar.setEvents(data.events());
    var right = new VBox(18, profile(data.profile()), calendar);
    left.setMinWidth(0);
    right.setPrefWidth(310);
    right.setMinWidth(255);
    HBox.setHgrow(left, Priority.ALWAYS);
    var columns = new HBox(18, left, right);
    columns.getStyleClass().add("dashboard-columns");
    page.getChildren().setAll(hero, message, refresh, columns);
  }

  private VBox deadlineRows(List<StudentAssessment> assessments) {
    var rows = new VBox(8);
    var upcoming = assessments.stream().filter(item -> !"Graded".equals(item.status())).limit(4).toList();
    if (upcoming.isEmpty()) rows.getChildren().add(new Label("No upcoming assessment deadlines."));
    for (var item : upcoming) {
      var title = DashboardUi.text(item.title(), "field-label");
      title.setWrapText(true);
      var due = DashboardUi.text(item.course() + " · " +
        item.deadline().format(DATE) + " · " + item.status(), "muted");
      due.setWrapText(true);
      var button = new Button();
      button.setGraphic(new VBox(4, title, due));
      button.setMaxWidth(Double.MAX_VALUE);
      button.setPrefHeight(82);
      button.setMaxHeight(82);
      button.getStyleClass().add("dashboard-action");
      button.setOnAction(e -> navigate.accept(Screen.STUDENT_ASSESSMENTS));
      rows.getChildren().add(button);
    }
    return rows;
  }

  private VBox announcementRows(List<ResourceItem> announcements) {
    var rows = new VBox(8);
    if (announcements.isEmpty()) rows.getChildren().add(new Label("No announcements from your enrolled courses yet."));
    for (var item : announcements.stream().limit(4).toList()) {
      var title = DashboardUi.text(item.title(), "field-label");
      title.setWrapText(true);
      String summary = item.description() == null || item.description().isBlank()
        ? item.course() + " · " + item.faculty() : item.description();
      var detail = DashboardUi.text(summary, "muted");
      detail.setWrapText(true);
      var date = DashboardUi.text(item.publishedAt() == null ? "Date not available" :
        CourseUi.date(item.publishedAt()), "muted");
      var button = new Button();
      button.setGraphic(new VBox(4, title, detail, date));
      button.setMaxWidth(Double.MAX_VALUE);
      button.getStyleClass().add("dashboard-action");
      button.setOnAction(e -> {
        var dialog = new Alert(Alert.AlertType.INFORMATION,
          item.description() == null ? "" : item.description(), ButtonType.OK);
        dialog.setTitle(item.title());
        dialog.setHeaderText(item.title());
        dialog.showAndWait();
      });
      rows.getChildren().add(button);
    }
    return rows;
  }

  private VBox profile(StudentProfile profile) {
    var name = DashboardUi.text(profile.name(), "panel-title");
    var roll = DashboardUi.text("Roll No. " + profile.enrollmentNumber(), "muted");
    var program = new Label(profile.program() == null ? "Program not available" : profile.program());
    program.setWrapText(true);
    var year = new Label("Year " + (profile.academicYear() == null ? "Not available" : profile.academicYear()) +
      " · Semester " + (profile.semester() == null ? "Not available" : profile.semester()));
    var settings = link("Edit profile →", Screen.STUDENT_SETTINGS);
    var panel = DashboardUi.panel("Student profile", name, roll, program, year, settings);
    if (profile.synthetic()) panel.getChildren().add(DashboardUi.text("Synthetic local demo record", "muted"));
    return panel;
  }

  private Button link(String text, Screen destination) {
    var button = new Button(text);
    button.getStyleClass().add("link-button");
    button.setOnAction(e -> navigate.accept(destination));
    return button;
  }
}
