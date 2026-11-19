package com.thapar.ams.desktop.ui;

import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.ui.faculty.FacultyScheduleView;
import java.time.LocalDate;
import java.time.ZoneId;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class CalendarView extends ScrollPane {
  private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
  private final WorkspaceApi api;
  private final TaskRunner tasks;
  private final boolean faculty;
  private final CalendarWidget calendar = new CalendarWidget();
  private final Label message = new Label();
  private final TextField title = new TextField();
  private final DatePicker date = new DatePicker(LocalDate.now(CAMPUS));
  private final Button add = new Button("Add event");

  public CalendarView(WorkspaceApi api, CourseApi courses, TaskRunner tasks, boolean faculty) {
    this.api = api;
    this.tasks = tasks;
    this.faculty = faculty;
    var refresh = new Button("Refresh calendar");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    var content = new VBox(18);
    content.setMaxWidth(Double.MAX_VALUE);
    content.getChildren().add(calendar);
    if (faculty) {
      content.getChildren().add(new FacultyScheduleView(courses, tasks));
      var columns = new HBox(20, content, eventPanel());
      columns.getStyleClass().add("calendar-columns");
      HBox.setHgrow(content, Priority.ALWAYS);
      CourseUi.fill(this, CourseUi.page("Calendar", "Browse academic dates and manage your classes.",
        message, refresh, columns));
    } else {
      content.setMaxWidth(760);
      CourseUi.fill(this, CourseUi.page("Calendar", "Your classes, deadlines, and academic events.",
        message, refresh, content));
    }
    load("");
  }

  private VBox eventPanel() {
    title.setPromptText("Academic event title");
    date.setMaxWidth(Double.MAX_VALUE);
    add.getStyleClass().add("primary-button");
    add.setOnAction(e -> addEvent());
    var panel = CourseUi.panel("Add event", CourseUi.label("Event title"), title,
      CourseUi.label("Date"), date, add);
    panel.getStyleClass().add("calendar-event-form");
    return panel;
  }

  private void load(String notice) {
    message.setText("Loading calendar…");
    tasks.run(faculty ? api::facultyEvents : api::studentEvents, events -> {
      calendar.setEvents(events);
      message.setText(notice);
    }, error -> message.setText(error.getMessage()));
  }

  private void addEvent() {
    if (title.getText().isBlank() || title.getText().length() > 255 || date.getValue() == null) {
      message.setText("Enter an event title of at most 255 characters and choose a date.");
      return;
    }
    String heading = title.getText().trim();
    LocalDate chosen = date.getValue();
    add.setDisable(true);
    message.setText("Adding event…");
    tasks.run(() -> { api.addEvent(heading, chosen); return null; }, ignored -> {
      add.setDisable(false);
      title.clear();
      calendar.selectDate(chosen);
      load("Event added to your calendar.");
    }, error -> {
      add.setDisable(false);
      message.setText(error.getMessage());
    });
  }
}
