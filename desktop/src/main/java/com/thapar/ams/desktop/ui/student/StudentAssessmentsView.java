package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.desktop.api.AssessmentApi;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class StudentAssessmentsView extends ScrollPane {
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
  private final AssessmentApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final VBox assessments = new VBox(18);

  public StudentAssessmentsView(AssessmentApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var page = new VBox(20);
    page.getStyleClass().add("module-page");
    var heading = new Label("Assessments");
    heading.getStyleClass().add("module-title");
    var subtitle = new Label("Stay on top of deadlines, submissions, and published feedback.");
    subtitle.getStyleClass().add("muted");
    message.getStyleClass().add("feedback");
    message.setWrapText(true);
    var refresh = new Button("Refresh assessments");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    page.getChildren().addAll(heading, subtitle, message, refresh, assessments);
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load("");
  }

  private void load(String notice) {
    message.setText(notice.isBlank() ? "Loading assessments…" : notice);
    tasks.run(api::studentAssessments, items -> {
      show(items);
      message.setText(notice);
    }, error -> message.setText(error.getMessage()));
  }

  private void show(List<StudentAssessment> items) {
    assessments.getChildren().clear();
    if (items.isEmpty()) assessments.getChildren().add(new Label("No assessments in your enrolled courses."));
    for (var item : items) {
      var title = new Label(item.title());
      title.getStyleClass().add("panel-title");
      title.setWrapText(true);
      HBox.setHgrow(title, Priority.ALWAYS);
      var status = new Label(item.status());
      status.getStyleClass().add(item.marks() != null ? "status-published" : "status-draft");
      var header = new HBox(14, title, status);
      var course = new Label(item.course() + " · " + item.kind());
      course.getStyleClass().add("muted");
      var deadline = new Label("Due " + date(item.deadline()));
      var card = new VBox(14, header, course, deadline);
      card.getStyleClass().add("panel");
      if (item.submissionId() != null) submitted(card, item);
      else if ("Overdue".equals(item.status())) card.getChildren().add(new Label("The submission deadline has passed."));
      else submissionForm(card, item);
      assessments.getChildren().add(card);
    }
  }

  private void submitted(VBox card, StudentAssessment item) {
    card.getChildren().add(new Label("Submitted " + date(item.submittedAt())));
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
    if (item.marks() == null) {
      var pending = new Label("Marks and feedback will appear after faculty publication.");
      pending.getStyleClass().add("muted");
      card.getChildren().add(pending);
    } else {
      var marks = new Label(item.marks() + " / 100");
      marks.getStyleClass().add("assessment-title");
      var feedback = new Label(item.feedback() == null || item.feedback().isBlank()
        ? "No faculty feedback provided." : item.feedback());
      feedback.setWrapText(true);
      card.getChildren().addAll(marks, feedback);
    }
  }

  private void submissionForm(VBox card, StudentAssessment item) {
    var content = new TextArea();
    content.setPromptText("Your response (or attach a supported file below)");
    content.setPrefRowCount(4);
    content.setWrapText(true);
    var selected = new Label("No file selected");
    selected.getStyleClass().add("muted");
    var choose = new Button("Choose PDF/TXT file");
    choose.getStyleClass().add("secondary-button");
    final File[] attachment = new File[1];
    choose.setOnAction(e -> {
      var picker = new FileChooser();
      picker.setTitle("Choose assignment file");
      picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF or text", "*.pdf", "*.txt"));
      File file = picker.showOpenDialog(getScene().getWindow());
      if (file != null) { attachment[0] = file; selected.setText(file.getName()); }
    });
    var submit = new Button("Submit assignment");
    submit.getStyleClass().add("primary-button");
    submit.setOnAction(e -> {
      if (content.getText().length() > 4000) {
        message.setText("Submission text must be at most 4000 characters."); return;
      }
      if (content.getText().isBlank() && attachment[0] == null) {
        message.setText("Enter submission text or choose a PDF/TXT file."); return;
      }
      submit.setDisable(true);
      message.setText("Submitting assignment…");
      String response = content.getText();
      var file = attachment[0] == null ? null : attachment[0].toPath();
      tasks.run(() -> { api.submit(item.id(), response, file); return null; },
        ignored -> load("Assignment submitted."), error -> {
          submit.setDisable(false);
          message.setText(error.getMessage());
        });
    });
    var note = new Label("PDF or UTF-8 TXT, up to 5 MB. Your course faculty can download submitted work.");
    note.getStyleClass().add("muted");
    note.setWrapText(true);
    card.getChildren().addAll(label("Submission text"), content, label("Assignment file"),
      new HBox(10, choose, selected), note, submit);
  }

  private void download(StudentAssessment item) {
    var chooser = new FileChooser();
    chooser.setTitle("Save your submission");
    chooser.setInitialFileName(item.fileName());
    var file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) return;
    message.setText("Downloading submission…");
    tasks.run(() -> { Files.write(file.toPath(), api.studentFile(item.submissionId())); return null; },
      ignored -> message.setText("Saved " + file.getName() + "."),
      error -> message.setText(error.getMessage()));
  }

  private static Label label(String text) {
    var label = new Label(text);
    label.getStyleClass().add("field-label");
    return label;
  }

  private static String date(LocalDateTime value) { return value == null ? "Date unavailable" : value.format(DATE_TIME); }
}
