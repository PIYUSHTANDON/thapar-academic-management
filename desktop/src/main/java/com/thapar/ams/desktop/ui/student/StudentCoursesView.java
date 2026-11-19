package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.contract.CourseContracts.StudentCourse;
import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.nio.file.Files;
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class StudentCoursesView extends ScrollPane {
  private final CourseApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final VBox courseList = new VBox(12);
  private final VBox detail = new VBox(20);
  private final VBox resources = new VBox(10);
  private final VBox schedule = new VBox(8);
  private List<ClassSessionItem> sessions = List.of();
  private StudentCourse selected;

  public StudentCoursesView(CourseApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var refresh = new Button("Refresh courses");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    courseList.getStyleClass().add("course-list");
    var columns = new HBox(20, courseList, detail);
    columns.getStyleClass().add("course-columns");
    HBox.setHgrow(detail, Priority.ALWAYS);
    detail.setMaxWidth(Double.MAX_VALUE);
    CourseUi.fill(this, CourseUi.page("Courses & LMS", "Your academics, connected in one place.",
      message, refresh, columns));
    load();
  }

  private void load() {
    message.setText("Loading courses…");
    tasks.run(() -> new State(api.studentCourses(), api.studentSchedule()), state -> {
      Long selectedId = selected == null ? null : selected.id();
      sessions = state.schedule();
      courseList.getChildren().clear();
      if (state.courses().isEmpty()) {
        selected = null;
        detail.getChildren().clear();
        courseList.getChildren().add(new Label("No enrolled courses."));
      }
      for (var course : state.courses()) courseList.getChildren().add(courseButton(course));
      state.courses().stream().filter(c -> selectedId != null && c.id() == selectedId).findFirst()
        .or(() -> state.courses().stream().findFirst()).ifPresent(this::select);
      message.setText("");
    }, error -> message.setText(error.getMessage()));
  }

  private Button courseButton(StudentCourse course) {
    var code = new Label(course.code() + " · " + course.section());
    code.getStyleClass().add("muted");
    var name = new Label(course.title());
    name.getStyleClass().add("course-name");
    name.setWrapText(true);
    var faculty = new Label(course.faculty());
    faculty.getStyleClass().add("muted");
    var button = new Button();
    button.setGraphic(new VBox(8, code, name, faculty));
    button.setUserData(course.id());
    button.setMaxWidth(Double.MAX_VALUE);
    button.setPrefHeight(140);
    button.setMaxHeight(140);
    button.getStyleClass().add("course-card");
    button.setOnAction(e -> select(course));
    return button;
  }

  private void select(StudentCourse course) {
    selected = course;
    for (var node : courseList.getChildren()) {
      if (node instanceof Button button) {
        button.getStyleClass().remove("selected");
        if (button.getUserData().equals(course.id())) button.getStyleClass().add("selected");
      }
    }
    var description = new Label(course.description() == null || course.description().isBlank()
      ? "No course description available." : course.description());
    description.setWrapText(true);
    var faculty = new Label("Faculty: " + course.faculty());
    faculty.getStyleClass().add("muted");
    detail.getChildren().setAll(CourseUi.panel("Course information", description, faculty),
      CourseUi.panel("Learning resources & announcements", resources),
      CourseUi.panel("Course schedule", schedule));
    showSchedule(course.id());
    loadResources(course.id());
  }

  private void loadResources(long courseId) {
    resources.getChildren().setAll(new Label("Loading resources…"));
    tasks.run(() -> api.studentResources(courseId), items -> {
      if (selected == null || selected.id() != courseId) return;
      showResources(items);
    }, error -> { if (selected != null && selected.id() == courseId) resources.getChildren().setAll(new Label(error.getMessage())); });
  }

  private void showResources(List<ResourceItem> items) {
    resources.getChildren().clear();
    if (items.isEmpty()) resources.getChildren().add(new Label("No course resources or announcements yet."));
    for (var item : items) {
      var date = new Label(item.kind() + " · " + (item.publishedAt() == null ? "Date not available" : CourseUi.date(item.publishedAt())));
      date.getStyleClass().add("muted");
      var title = new Label(item.title());
      title.getStyleClass().add("field-label");
      title.setWrapText(true);
      var row = new VBox(5, date, title);
      row.getStyleClass().add("resource-row");
      if (item.description() != null && !item.description().isBlank()) {
        var summary = new Label(item.description());
        summary.getStyleClass().add("muted");
        summary.setWrapText(true);
        row.getChildren().add(summary);
      }
      var action = new Button("File".equals(item.kind()) ? "Download" : "Read announcement");
      action.getStyleClass().add("link-button");
      action.setOnAction(e -> {
        if ("File".equals(item.kind())) saveFile(item);
        else {
          var dialog = new Alert(Alert.AlertType.INFORMATION,
            item.description() == null ? "" : item.description(), ButtonType.OK);
          dialog.setTitle(item.title());
          dialog.setHeaderText(item.title());
          dialog.showAndWait();
        }
      });
      row.getChildren().add(action);
      resources.getChildren().add(row);
    }
  }

  private void showSchedule(long courseId) {
    schedule.getChildren().clear();
    var rows = sessions.stream().filter(s -> s.courseId() == courseId).toList();
    if (rows.isEmpty()) schedule.getChildren().add(new Label("No classes scheduled."));
    for (var session : rows) {
      var when = new Label(CourseUi.date(session.startsAt()));
      when.getStyleClass().add("field-label");
      var room = new Label(session.room() + " · " + session.status());
      room.getStyleClass().add("muted");
      var row = new HBox(20, when, room);
      row.getStyleClass().add("schedule-row");
      schedule.getChildren().add(row);
    }
  }

  private void saveFile(ResourceItem resource) {
    var picker = new FileChooser();
    picker.setTitle("Save learning resource");
    picker.setInitialFileName(resource.title());
    var destination = picker.showSaveDialog(getScene().getWindow());
    if (destination == null) return;
    message.setText("Downloading resource…");
    tasks.run(() -> { Files.write(destination.toPath(), api.studentFile(resource.id())); return null; },
      ignored -> message.setText("Saved " + destination.getName() + "."),
      error -> message.setText(error.getMessage()));
  }

  private record State(List<StudentCourse> courses, List<ClassSessionItem> schedule) { }
}
