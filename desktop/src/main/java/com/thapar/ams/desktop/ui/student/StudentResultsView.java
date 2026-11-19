package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.desktop.api.AssessmentApi;
import com.thapar.ams.desktop.ui.TaskRunner;
import com.thapar.ams.desktop.ui.UiFeedback;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class StudentResultsView extends ScrollPane {
  private final AssessmentApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final Label cgpa = new Label();
  private final VBox results = new VBox(12);

  public StudentResultsView(AssessmentApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var page = new VBox(20);
    page.getStyleClass().add("module-page");
    var title = new Label("Results");
    title.getStyleClass().add("module-title");
    var subtitle = new Label("Published assessment marks and official academic information.");
    subtitle.getStyleClass().add("muted");
    message.getStyleClass().add("feedback");
    var refresh = new Button("Refresh results");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    var record = panel("Official academic record");
    cgpa.getStyleClass().add("assessment-title");
    var note = new Label("Official CGPA comes from your academic record. Assessment marks do not calculate CGPA.");
    note.getStyleClass().add("muted");
    note.setWrapText(true);
    record.getChildren().addAll(cgpa, note);
    var published = panel("Published assessment results");
    published.getChildren().add(results);
    page.getChildren().addAll(title, subtitle, message, refresh, record, published);
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load();
  }

  private void load() {
    UiFeedback.loading(message, "Loading results…");
    tasks.run(api::results, data -> {
      cgpa.setText("Current CGPA: " + (data.officialCgpa() == null ? "Not available" : data.officialCgpa()));
      results.getChildren().clear();
      if (data.results().isEmpty()) results.getChildren().add(UiFeedback.empty("No published results yet."));
      for (var item : data.results()) {
        var name = new Label(item.course() + " · " + item.title());
        name.getStyleClass().add("assessment-title");
        name.setWrapText(true);
        var detail = new Label(item.kind() + "  ·  " + item.marks() + " / 100");
        var feedback = new Label(item.feedback() == null || item.feedback().isBlank()
          ? "No feedback provided." : item.feedback());
        feedback.setWrapText(true);
        var row = new VBox(7, name, detail, feedback);
        row.getStyleClass().add("assessment-card");
        results.getChildren().add(row);
      }
      UiFeedback.clear(message);
    }, error -> UiFeedback.error(message, error));
  }

  private static VBox panel(String heading) {
    var title = new Label(heading);
    title.getStyleClass().add("panel-title");
    var panel = new VBox(15, title);
    panel.getStyleClass().add("panel");
    return panel;
  }
}
