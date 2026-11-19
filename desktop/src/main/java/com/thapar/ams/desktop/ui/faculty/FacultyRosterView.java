package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import com.thapar.ams.desktop.ui.UiFeedback;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class FacultyRosterView extends ScrollPane {
  private final CourseApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final TextField query = new TextField();
  private final ComboBox<CourseChoice> course = new ComboBox<>();
  private final VBox rows = new VBox();
  private List<RosterEntry> roster = List.of();
  private boolean loaded;

  public FacultyRosterView(CourseApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    query.setPromptText("Search name or enrollment number");
    query.setMaxWidth(Double.MAX_VALUE);
    query.textProperty().addListener((changed, oldValue, newValue) -> filter());
    course.valueProperty().addListener((changed, oldValue, newValue) -> filter());
    course.setMaxWidth(Double.MAX_VALUE);
    var refresh = new Button("Refresh roster");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    var filters = new HBox(12, query, course);
    filters.getStyleClass().add("roster-filters");
    HBox.setHgrow(query, Priority.ALWAYS);
    HBox.setHgrow(course, Priority.ALWAYS);
    CourseUi.fill(this, CourseUi.page("Student Roster", "Academic information for students enrolled in your courses.",
      message, refresh, CourseUi.panel("Enrolled students", filters, rows)));
    load();
  }

  private void load() {
    UiFeedback.loading(message, "Loading roster…");
    tasks.run(() -> new State(api.facultyCourses(), api.roster()), state -> {
      Long previous = course.getValue() == null ? null : course.getValue().id();
      roster = state.roster();
      loaded = true;
      course.getItems().clear();
      course.getItems().add(new CourseChoice(null, "All assigned courses"));
      for (var item : state.courses())
        course.getItems().add(new CourseChoice(item.id(), item.title() + " · " + item.section()));
      course.getSelectionModel().select(course.getItems().stream()
        .filter(item -> java.util.Objects.equals(item.id(), previous)).findFirst().orElse(course.getItems().getFirst()));
      filter();
      UiFeedback.clear(message);
    }, error -> UiFeedback.error(message, error));
  }

  private void filter() {
    rows.getChildren().clear();
    if (!loaded) return;
    String search = query.getText().trim().toLowerCase(Locale.ROOT);
    Long courseId = course.getValue() == null ? null : course.getValue().id();
    var filtered = roster.stream().filter(item -> (courseId == null || item.courseId() == courseId) &&
      (item.name().toLowerCase(Locale.ROOT).contains(search) ||
       item.enrollmentNumber().toLowerCase(Locale.ROOT).contains(search))).toList();
    if (filtered.isEmpty()) {
      rows.getChildren().add(UiFeedback.empty(roster.isEmpty()
        ? "No students are enrolled in your assigned courses."
        : "No students match this selection."));
      return;
    }
    var heading = row("Student", "Enrollment", "Program", "Course");
    heading.getStyleClass().add("attendance-heading");
    rows.getChildren().add(heading);
    for (var item : filtered) rows.getChildren().add(row(item.name(), item.enrollmentNumber(),
      item.program(), item.course()));
  }

  private static HBox row(String name, String enrollment, String program, String subject) {
    var row = new HBox(12);
    row.getStyleClass().add("attendance-row");
    for (var value : List.of(name, enrollment, program, subject)) {
      var label = new Label(value);
      label.setWrapText(true);
      label.setMinWidth(0);
      label.setPrefWidth(150);
      label.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(label, Priority.ALWAYS);
      row.getChildren().add(label);
    }
    return row;
  }

  private record CourseChoice(Long id, String label) {
    @Override public String toString() { return label; }
  }
  private record State(List<FacultyCourse> courses, List<RosterEntry> roster) { }
}
