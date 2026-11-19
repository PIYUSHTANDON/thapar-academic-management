package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class FacultyScheduleView extends VBox {
  private final CourseApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final ComboBox<ClassSessionItem> sessions = new ComboBox<>();
  private final DatePicker startDate = new DatePicker();
  private final DatePicker endDate = new DatePicker();
  private final TextField startTime = new TextField();
  private final TextField endTime = new TextField();
  private final TextField room = new TextField();
  private final CheckBox cancelled = new CheckBox();
  private final Button save = new Button("Save class information");

  public FacultyScheduleView(CourseApi api, TaskRunner tasks) {
    super(14);
    this.api = api;
    this.tasks = tasks;
    sessions.setMaxWidth(Double.MAX_VALUE);
    CourseUi.formatChoices(sessions, item -> item.course() + " · " + item.startsAt().toString().replace('T', ' '));
    sessions.valueProperty().addListener((changed, oldValue, selected) -> show(selected));
    startTime.setPromptText("HH:mm");
    endTime.setPromptText("HH:mm");
    room.setPromptText("Class room");
    save.getStyleClass().add("primary-button");
    save.setOnAction(e -> save());
    var refresh = new Button("Refresh classes");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    var cancellation = new HBox(8, cancelled, CourseUi.label("Cancel this class"));
    cancellation.getStyleClass().add("course-upload");
    var editor = CourseUi.panel("Manage class information", CourseUi.label("Teaching session"), sessions,
      CourseUi.label("Class start (campus time)"), new HBox(8, startDate, startTime),
      CourseUi.label("Class end (campus time)"), new HBox(8, endDate, endTime),
      CourseUi.label("Class room"), room, cancellation, save);
    editor.getStyleClass().add("schedule-editor");
    message.getStyleClass().add("feedback");
    getChildren().addAll(refresh, message, editor);
    load("");
  }

  private void load(String notice) {
    message.setText("Loading classes…");
    tasks.run(api::facultySchedule, items -> {
      Long previous = sessions.getValue() == null ? null : sessions.getValue().id();
      sessions.getItems().setAll(items);
      items.stream().filter(item -> previous != null && item.id() == previous).findFirst()
        .or(() -> items.stream().findFirst()).ifPresent(sessions::setValue);
      save.setDisable(items.isEmpty());
      if (items.isEmpty()) message.setText("No classes scheduled.");
      else message.setText(notice);
    }, error -> message.setText(error.getMessage()));
  }

  private void show(ClassSessionItem session) {
    if (session == null) return;
    var start = session.startsAt();
    var end = session.endsAt();
    startDate.setValue(start.toLocalDate());
    endDate.setValue(end.toLocalDate());
    startTime.setText(start.toLocalTime().toString());
    endTime.setText(end.toLocalTime().toString());
    room.setText(session.room());
    cancelled.setSelected("Cancelled".equalsIgnoreCase(session.status()));
  }

  private void save() {
    var selected = sessions.getValue();
    if (selected == null) return;
    LocalDateTime start, end;
    try {
      start = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTime.getText().trim()));
      end = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTime.getText().trim()));
    } catch (DateTimeParseException | NullPointerException invalid) {
      message.setText("Choose dates and enter times as HH:mm.");
      return;
    }
    if (!end.isAfter(start)) { message.setText("Class end must be after its start."); return; }
    if (room.getText().isBlank() || room.getText().length() > 100) {
      message.setText("Enter a room of at most 100 characters."); return;
    }
    long id = selected.id();
    String location = room.getText().trim();
    boolean isCancelled = cancelled.isSelected();
    save.setDisable(true);
    message.setText("Saving…");
    tasks.run(() -> { api.updateSession(id, start, end, location, isCancelled); return null; },
      ignored -> load("Class information updated."), error -> {
        save.setDisable(false);
        message.setText(error.getMessage());
      });
  }
}
