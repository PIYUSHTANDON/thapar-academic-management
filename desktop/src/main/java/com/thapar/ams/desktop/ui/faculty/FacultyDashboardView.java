package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.OverviewContracts.FacultyAction;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.api.WorkspaceApi.FacultyData;
import com.thapar.ams.desktop.navigation.Screen;
import com.thapar.ams.desktop.ui.CalendarWidget;
import com.thapar.ams.desktop.ui.DashboardUi;
import com.thapar.ams.desktop.ui.DashboardUi.Stat;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class FacultyDashboardView extends ScrollPane {
  private final WorkspaceApi api;
  private final TaskRunner tasks;
  private final UserResponse user;
  private final Consumer<Screen> navigate;
  private final Label message = new Label();
  private final VBox page = new VBox(18);

  public FacultyDashboardView(WorkspaceApi api, TaskRunner tasks, UserResponse user,
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
    message.setText("Loading your faculty workspace…");
    tasks.run(api::facultyDashboard, data -> {
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

  private void render(FacultyData data) {
    var summary = data.summary();
    var hero = DashboardUi.hero(user, "Together towards excellence.", List.of(
      new Stat("Active Classes", Long.toString(summary.activeClasses())),
      new Stat("Total Students", Long.toString(summary.totalStudents())),
      new Stat("Attendance", summary.attendancePercentage() + "%"),
      new Stat("Pending Reviews", Long.toString(summary.pendingReviews()))),
      data.notifications(), api, tasks, message);
    var refresh = new Button("Refresh dashboard");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    var quick = DashboardUi.actions(
      DashboardUi.action("Take Attendance", Screen.FACULTY_ATTENDANCE, navigate),
      DashboardUi.action("Create Assessment", Screen.FACULTY_ASSESSMENTS, navigate),
      DashboardUi.action("View Reports", Screen.FACULTY_REPORTS, navigate),
      DashboardUi.action("Manage Courses", Screen.FACULTY_COURSES, navigate));
    var calendar = new CalendarWidget();
    calendar.setEvents(data.events());
    var calendarLink = new Button("View full calendar →");
    calendarLink.getStyleClass().add("link-button");
    calendarLink.setOnAction(e -> navigate.accept(Screen.FACULTY_CALENDAR));
    var left = new VBox(18,
      DashboardUi.panel("Quick Access", quick),
      DashboardUi.panel("Today's Teaching Schedule", DashboardUi.today(data.schedule(),
        "No classes scheduled today. Enjoy time for research and preparation."), calendarLink));
    var right = new VBox(18, calendar, actionPanel(summary.actions()));
    left.setMinWidth(0);
    right.setPrefWidth(310);
    right.setMinWidth(255);
    HBox.setHgrow(left, Priority.ALWAYS);
    var columns = new HBox(18, left, right);
    columns.getStyleClass().add("dashboard-columns");
    page.getChildren().setAll(hero, message, refresh, columns);
  }

  private VBox actionPanel(List<FacultyAction> actions) {
    var rows = new VBox(8);
    if (actions == null || actions.isEmpty()) rows.getChildren().add(new Label("No pending academic actions."));
    else for (var action : actions) {
      var title = DashboardUi.text(action.title(), "field-label");
      title.setWrapText(true);
      var detail = DashboardUi.text(action.course() + " · " + action.status(), "muted");
      var button = new Button();
      button.setGraphic(new VBox(4, title, detail));
      button.setMaxWidth(Double.MAX_VALUE);
      button.getStyleClass().add("dashboard-action");
      Screen destination = destination(action.path());
      button.setDisable(destination == null);
      button.setOnAction(e -> navigate.accept(destination));
      rows.getChildren().add(button);
    }
    return DashboardUi.panel("Action Required", rows);
  }

  private static Screen destination(String path) {
    return switch (path) {
      case "/faculty/assessments" -> Screen.FACULTY_ASSESSMENTS;
      case "/faculty/attendance" -> Screen.FACULTY_ATTENDANCE;
      case "/faculty/courses" -> Screen.FACULTY_COURSES;
      case "/faculty/calendar" -> Screen.FACULTY_CALENDAR;
      default -> null;
    };
  }
}
