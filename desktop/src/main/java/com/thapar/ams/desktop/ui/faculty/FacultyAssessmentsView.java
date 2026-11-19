package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.AssessmentContracts.FacultyAssessment;
import com.thapar.ams.contract.AssessmentContracts.FacultySubmission;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.desktop.api.AssessmentApi;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class FacultyAssessmentsView extends ScrollPane {
  private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
  private final AssessmentApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final VBox assessments = new VBox(12);
  private final VBox submissions = new VBox(12);
  private final ComboBox<FacultyCourse> course = new ComboBox<>();
  private final TextField title = new TextField();
  private final ComboBox<String> kind = new ComboBox<>();
  private final DatePicker dueDate = new DatePicker(LocalDate.now(CAMPUS).plusDays(1));
  private final TextField dueTime = new TextField("23:59");
  private final Button create = new Button("Create assessment");

  public FacultyAssessmentsView(AssessmentApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var page = new VBox(20);
    page.getStyleClass().add("module-page");
    var heading = new Label("Assessments");
    heading.getStyleClass().add("module-title");
    var subtitle = new Label("Create assessments, review student work, and publish feedback.");
    subtitle.getStyleClass().add("muted");
    message.getStyleClass().add("feedback");
    message.setWrapText(true);
    var refresh = new Button("Refresh assessments");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    var left = new VBox(20, panel("Assessments", assessments), panel("Student submissions", submissions));
    HBox.setHgrow(left, Priority.ALWAYS);
    left.setMaxWidth(Double.MAX_VALUE);
    var columns = new HBox(20, left, createPanel());
    columns.getStyleClass().add("assessment-columns");
    page.getChildren().addAll(heading, subtitle, message, refresh, columns);
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load("");
  }

  private VBox createPanel() {
    var box = panel("Create assessment");
    box.getStyleClass().add("assessment-create");
    course.setMaxWidth(Double.MAX_VALUE);
    CourseUi.formatChoices(course, item -> item.code() + " · " + item.section() + " · " + item.title());
    title.setPromptText("e.g. Data structures problem set");
    kind.getItems().setAll("Assignment", "Quiz");
    kind.getSelectionModel().selectFirst();
    kind.setMaxWidth(Double.MAX_VALUE);
    dueTime.setPromptText("HH:mm");
    dueDate.setPrefWidth(155);
    var when = new HBox(8, dueDate, dueTime);
    HBox.setHgrow(dueDate, Priority.ALWAYS);
    dueTime.setPrefWidth(80);
    create.getStyleClass().add("primary-button");
    create.setMaxWidth(Double.MAX_VALUE);
    create.setOnAction(e -> create());
    box.getChildren().addAll(label("Course"), course, label("Title"), title,
      label("Type"), kind, label("Deadline (campus time)"), when, create);
    return box;
  }

  private void load(String notice) {
    message.setText(notice.isBlank() ? "Loading assessments…" : notice);
    tasks.run(() -> new State(api.facultyCourses(), api.facultyAssessments(), api.facultySubmissions()), state -> {
      Long selected = course.getValue() == null ? null : course.getValue().id();
      course.getItems().setAll(state.courses());
      course.getItems().stream().filter(item -> selected != null && item.id() == selected)
        .findFirst().ifPresentOrElse(course::setValue, () -> {
          if (!course.getItems().isEmpty()) course.getSelectionModel().selectFirst();
        });
      create.setDisable(state.courses().isEmpty());
      showAssessments(state.assessments());
      showSubmissions(state.submissions());
      message.setText(notice);
    }, error -> message.setText(error.getMessage()));
  }

  private void showAssessments(List<FacultyAssessment> items) {
    assessments.getChildren().clear();
    if (items.isEmpty()) assessments.getChildren().add(new Label("No assessments yet."));
    for (var item : items) {
      var name = new Label(item.title());
      name.getStyleClass().add("assessment-title");
      name.setWrapText(true);
      var detail = new Label(item.course() + " · " + item.kind() + " · Due " + date(item.deadline()));
      detail.getStyleClass().add("muted");
      detail.setWrapText(true);
      var status = new Label(item.published() ? "Published" : "Draft");
      status.getStyleClass().add(item.published() ? "status-published" : "status-draft");
      var publish = new Button(item.published() ? "Publish updates" : "Publish results");
      publish.getStyleClass().add("secondary-button");
      publish.setOnAction(e -> action(() -> { api.publish(item.id()); return null; }, publish,
        "Results published."));
      var row = new HBox(12, status, publish);
      var card = new VBox(10, name, detail, row);
      card.getStyleClass().add("assessment-card");
      assessments.getChildren().add(card);
    }
  }

  private void showSubmissions(List<FacultySubmission> items) {
    submissions.getChildren().clear();
    if (items.isEmpty()) submissions.getChildren().add(new Label("No submissions received."));
    for (var item : items) {
      var name = new Label(item.assessment());
      name.getStyleClass().add("assessment-title");
      name.setWrapText(true);
      var student = new Label(item.student());
      student.getStyleClass().add("muted");
      var card = new VBox(10, name, student);
      card.getStyleClass().add("assessment-card");
      if (item.content() != null && !item.content().isBlank()) {
        var content = new Label(item.content());
        content.setWrapText(true);
        card.getChildren().add(content);
      }
      if (item.fileName() != null && !item.fileName().isBlank()) {
        var download = new Button("Download " + item.fileName());
        download.getStyleClass().add("link-button");
        download.setOnAction(e -> download(item));
        card.getChildren().add(download);
      }
      var marks = new TextField("Ungraded".equals(item.marks()) ? "" : item.marks());
      marks.setPromptText("0–100");
      marks.setMaxWidth(110);
      var feedback = new TextArea(item.feedback() == null ? "" : item.feedback());
      feedback.setPromptText("Feedback for the student");
      feedback.setPrefRowCount(3);
      feedback.setWrapText(true);
      var save = new Button("Save grade");
      save.getStyleClass().add("secondary-button");
      save.setOnAction(e -> {
        int value;
        try { value = Integer.parseInt(marks.getText().trim()); }
        catch (NumberFormatException invalid) { message.setText("Enter marks from 0 to 100."); return; }
        if (value < 0 || value > 100) { message.setText("Enter marks from 0 to 100."); return; }
        if (feedback.getText().length() > 2000) {
          message.setText("Feedback must be at most 2000 characters."); return;
        }
        int grade = value;
        String note = feedback.getText();
        action(() -> { api.grade(item.id(), grade, note); return null; }, save,
          "Grade saved. Publish updates to release it to the student.");
      });
      card.getChildren().addAll(label("Marks / 100"), marks, label("Faculty feedback"), feedback, save);
      submissions.getChildren().add(card);
    }
  }

  private void create() {
    if (course.getValue() == null || title.getText().isBlank() || title.getText().length() > 255) {
      message.setText("Choose a course and enter a title of at most 255 characters."); return;
    }
    LocalDateTime deadline;
    try { deadline = LocalDateTime.of(dueDate.getValue(), LocalTime.parse(dueTime.getText().trim())); }
    catch (DateTimeParseException | NullPointerException invalid) {
      message.setText("Choose a date and enter the time as HH:mm."); return;
    }
    if (!deadline.isAfter(LocalDateTime.now(CAMPUS))) {
      message.setText("The deadline must be in the future."); return;
    }
    long courseId = course.getValue().id();
    String name = title.getText().trim(), type = kind.getValue();
    LocalDateTime due = deadline;
    action(() -> { api.create(courseId, name, type, due); return null; }, create, "Assessment created.");
  }

  private void download(FacultySubmission item) {
    var chooser = new FileChooser();
    chooser.setTitle("Save student submission");
    chooser.setInitialFileName(item.fileName());
    var file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) return;
    message.setText("Downloading submission…");
    tasks.run(() -> { Files.write(file.toPath(), api.facultyFile(item.id())); return null; },
      ignored -> message.setText("Saved " + file.getName() + "."),
      error -> message.setText(error.getMessage()));
  }

  private void action(Callable<Void> work, Button button, String notice) {
    button.setDisable(true);
    message.setText("Saving…");
    tasks.run(work, ignored -> load(notice), error -> {
      button.setDisable(false);
      message.setText(error.getMessage());
    });
  }

  private static String date(LocalDateTime value) { return value.format(DATE_TIME); }

  private static Label label(String value) {
    var label = new Label(value);
    label.getStyleClass().add("field-label");
    return label;
  }

  private static VBox panel(String heading, javafx.scene.Node... content) {
    var title = new Label(heading);
    title.getStyleClass().add("panel-title");
    var box = new VBox(16, title);
    box.getChildren().addAll(content);
    box.getStyleClass().add("panel");
    return box;
  }

  private record State(List<FacultyCourse> courses, List<FacultyAssessment> assessments,
                       List<FacultySubmission> submissions) { }
}
