package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import com.thapar.ams.contract.CourseContracts.ResourceItem;
import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class FacultyCoursesView extends ScrollPane {
  private final CourseApi api;
  private final TaskRunner tasks;
  private final Label message = new Label();
  private final VBox courseList = new VBox(12);
  private final VBox detail = new VBox(20);
  private final VBox resources = new VBox(10);
  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final TextField announcementTitle = new TextField();
  private final TextArea announcementDescription = new TextArea();
  private final Label chosenFile = new Label("No file selected");
  private FacultyCourse selected;
  private Path file;

  public FacultyCoursesView(CourseApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var refresh = new Button("Refresh courses");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    courseList.getStyleClass().add("course-list");
    var columns = new HBox(20, courseList, detail);
    columns.getStyleClass().add("course-columns");
    HBox.setHgrow(detail, Priority.ALWAYS);
    detail.setMaxWidth(Double.MAX_VALUE);
    CourseUi.fill(this, CourseUi.page("Courses & LMS",
      "Your teaching, resources, and announcements in one place.", message, refresh, columns));
    load("");
  }

  private void load(String notice) {
    message.setText("Loading courses…");
    tasks.run(api::facultyCourses, courses -> {
      Long selectedId = selected == null ? null : selected.id();
      courseList.getChildren().clear();
      if (courses.isEmpty()) {
        selected = null;
        detail.getChildren().clear();
        courseList.getChildren().add(new Label("No courses assigned."));
      }
      for (var course : courses) {
        var button = courseButton(course);
        courseList.getChildren().add(button);
      }
      courses.stream().filter(c -> selectedId != null && c.id() == selectedId).findFirst()
        .or(() -> courses.stream().findFirst()).ifPresent(this::select);
      message.setText(notice);
    }, error -> message.setText(error.getMessage()));
  }

  private Button courseButton(FacultyCourse course) {
    var name = new Label(course.title());
    name.getStyleClass().add("course-name");
    name.setWrapText(true);
    var code = new Label(course.code() + " · " + course.section());
    code.getStyleClass().add("muted");
    var count = new Label(course.students() + " enrolled students");
    count.getStyleClass().add("muted");
    var button = new Button();
    button.setGraphic(new VBox(8, code, name, count));
    button.setUserData(course.id());
    button.setMaxWidth(Double.MAX_VALUE);
    button.setPrefHeight(140);
    button.setMaxHeight(140);
    button.getStyleClass().add("course-card");
    if (selected != null && selected.id() == course.id()) button.getStyleClass().add("selected");
    button.setOnAction(e -> select(course));
    return button;
  }

  private void select(FacultyCourse course) {
    if (selected == null || selected.id() != course.id()) {
      file = null;
      chosenFile.setText("No file selected");
      announcementTitle.clear();
      announcementDescription.clear();
    }
    selected = course;
    for (var node : courseList.getChildren()) {
      if (node instanceof Button button) {
        button.getStyleClass().remove("selected");
        if (button.getUserData().equals(course.id())) button.getStyleClass().add("selected");
      }
    }
    title.setText(course.title());
    description.setText(course.description() == null ? "" : course.description());
    detail.getChildren().setAll(informationPanel(), resourcesPanel(), announcementPanel());
    loadResources(course.id());
  }

  private VBox informationPanel() {
    description.setPrefRowCount(4);
    description.setWrapText(true);
    var save = new Button("Save course");
    save.getStyleClass().add("primary-button");
    save.setOnAction(e -> {
      if (selected == null || title.getText().isBlank() || title.getText().length() > 255 ||
          description.getText().length() > 2000) {
        message.setText("Enter a title of at most 255 characters and a description of at most 2000 characters.");
        return;
      }
      long id = selected.id();
      String name = title.getText().trim(), summary = description.getText();
      action(() -> { api.updateCourse(id, name, summary); return null; }, save,
        () -> load("Course updated."));
    });
    return CourseUi.panel("Course information", CourseUi.label("Course title"), title,
      CourseUi.label("Description"), description, save);
  }

  private VBox resourcesPanel() {
    var choose = new Button("Choose file");
    choose.getStyleClass().add("secondary-button");
    choose.setOnAction(e -> {
      var picker = new FileChooser();
      picker.setTitle("Choose learning resource");
      var chosen = picker.showOpenDialog(getScene().getWindow());
      if (chosen != null) { file = chosen.toPath(); chosenFile.setText(chosen.getName()); }
    });
    var upload = new Button("Upload");
    upload.getStyleClass().add("secondary-button");
    upload.setOnAction(e -> {
      if (selected == null || file == null) { message.setText("Choose a resource file first."); return; }
      long id = selected.id();
      Path chosen = file;
      action(() -> { api.upload(id, chosen); return null; }, upload, () -> {
        file = null;
        chosenFile.setText("No file selected");
        message.setText("Resource uploaded.");
        loadResources(id);
      });
    });
    var row = new HBox(10, choose, chosenFile, upload);
    row.getStyleClass().add("course-upload");
    HBox.setHgrow(chosenFile, Priority.ALWAYS);
    return CourseUi.panel("Learning resources", row, resources);
  }

  private VBox announcementPanel() {
    announcementTitle.setPromptText("What should your students know?");
    announcementDescription.setPrefRowCount(4);
    announcementDescription.setWrapText(true);
    var publish = new Button("Publish announcement");
    publish.getStyleClass().add("primary-button");
    publish.setOnAction(e -> {
      if (selected == null || announcementTitle.getText().isBlank() ||
          announcementTitle.getText().length() > 255 || announcementDescription.getText().length() > 4000) {
        message.setText("Enter an announcement title of at most 255 characters and description of at most 4000 characters.");
        return;
      }
      long id = selected.id();
      String heading = announcementTitle.getText().trim(), body = announcementDescription.getText();
      action(() -> { api.announce(id, heading, body); return null; }, publish, () -> {
        announcementTitle.clear();
        announcementDescription.clear();
        message.setText("Announcement shared with enrolled students.");
        loadResources(id);
      });
    });
    return CourseUi.panel("Create announcement", CourseUi.label("Announcement"), announcementTitle,
      CourseUi.label("Announcement description"), announcementDescription, publish);
  }

  private void loadResources(long courseId) {
    resources.getChildren().setAll(new Label("Loading resources…"));
    tasks.run(() -> api.facultyResources(courseId), items -> {
      if (selected == null || selected.id() != courseId) return;
      showResources(items);
    }, error -> { if (selected != null && selected.id() == courseId) resources.getChildren().setAll(new Label(error.getMessage())); });
  }

  private void showResources(List<ResourceItem> items) {
    resources.getChildren().clear();
    if (items.isEmpty()) resources.getChildren().add(new Label("No resources shared yet."));
    for (var resource : items) {
      var name = new Label(resource.title());
      name.getStyleClass().add("field-label");
      name.setWrapText(true);
      var kind = new Label(resource.kind());
      kind.getStyleClass().add("muted");
      var row = new VBox(5, kind, name);
      row.getStyleClass().add("resource-row");
      if ("File".equals(resource.kind())) {
        var download = new Button("Download");
        download.getStyleClass().add("link-button");
        download.setOnAction(e -> saveFile(resource));
        row.getChildren().add(download);
      }
      resources.getChildren().add(row);
    }
  }

  private void saveFile(ResourceItem resource) {
    var picker = new FileChooser();
    picker.setTitle("Save learning resource");
    picker.setInitialFileName(resource.title());
    var destination = picker.showSaveDialog(getScene().getWindow());
    if (destination == null) return;
    message.setText("Downloading resource…");
    tasks.run(() -> { Files.write(destination.toPath(), api.facultyFile(resource.id())); return null; },
      ignored -> message.setText("Saved " + destination.getName() + "."),
      error -> message.setText(error.getMessage()));
  }

  private void action(Callable<Void> work, Button button, Runnable success) {
    button.setDisable(true);
    message.setText("Saving…");
    tasks.run(work, ignored -> { button.setDisable(false); success.run(); }, error -> {
      button.setDisable(false);
      message.setText(error.getMessage());
    });
  }
}
