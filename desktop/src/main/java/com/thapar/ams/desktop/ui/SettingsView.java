package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.desktop.api.AccountApi;
import com.thapar.ams.desktop.api.WorkspaceApi;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingsView extends ScrollPane {
  private final AccountApi account;
  private final WorkspaceApi workspace;
  private final TaskRunner tasks;
  private final Consumer<UserResponse> userChanged;
  private final Runnable passwordChanged;
  private final String role;
  private final Label status = new Label();
  private final Label identifier = new Label();
  private final VBox academicDetails = new VBox(8);
  private final VBox notices = new VBox(8);
  private final TextField name = new TextField();
  private final CheckBox notificationsEnabled = new CheckBox("Enable notification preferences");
  private final PasswordField currentPassword = new PasswordField();
  private final PasswordField newPassword = new PasswordField();
  private final Button save = new Button("Save changes");
  private final Button change = new Button("Update password");
  private boolean busy;
  private boolean loaded;

  protected SettingsView(AccountApi account, WorkspaceApi workspace, TaskRunner tasks, UserResponse user,
                         Consumer<UserResponse> userChanged, Runnable passwordChanged, Runnable signOut) {
    this.account = account;
    this.workspace = workspace;
    this.tasks = tasks;
    this.userChanged = userChanged;
    this.passwordChanged = passwordChanged;
    role = user.role() == UserRole.FACULTY ? "faculty" : "student";
    status.getStyleClass().add("feedback");
    status.setWrapText(true);

    name.setPromptText("Full name");
    name.setAccessibleText("Full name");
    name.setOnAction(e -> save());
    identifier.getStyleClass().add("muted");
    save.getStyleClass().add("primary-button");
    save.setOnAction(e -> save());
    var profile = CourseUi.panel(role.equals("faculty") ? "Your profile" : "Student profile",
      CourseUi.label("Full name"), name, CourseUi.label("Institute ID"), identifier,
      academicDetails, notificationsEnabled, save);

    currentPassword.setPromptText("Current password");
    currentPassword.setAccessibleText("Current password");
    newPassword.setPromptText("New password, at least 12 characters");
    newPassword.setAccessibleText("New password");
    newPassword.setOnAction(e -> changePassword());
    change.getStyleClass().add("primary-button");
    change.setOnAction(e -> changePassword());
    var passwordHint = new Label("Use 12–128 characters. Changing your password signs you out.");
    passwordHint.getStyleClass().add("muted");
    passwordHint.setWrapText(true);
    var security = CourseUi.panel("Change password", CourseUi.label("Current password"),
      currentPassword, CourseUi.label("New password"), newPassword, passwordHint, change);
    var signOutButton = new Button("Sign out");
    signOutButton.getStyleClass().add("secondary-button");
    signOutButton.setOnAction(e -> signOut.run());
    security.getChildren().add(signOutButton);

    profile.setMinWidth(0);
    security.setMinWidth(0);
    profile.setPrefWidth(330);
    security.setPrefWidth(330);
    HBox.setHgrow(profile, Priority.ALWAYS);
    HBox.setHgrow(security, Priority.ALWAYS);
    var columns = new HBox(18, profile, security);
    columns.getStyleClass().add("settings-columns");
    var refresh = new Button("Refresh settings");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    var notificationPanel = CourseUi.panel("Notifications", notices);
    CourseUi.fill(this, CourseUi.page("Settings", "Manage your account and notification preferences.",
      status, refresh, columns, notificationPanel));
    load();
  }

  private void load() {
    loaded = false;
    setBusy(false);
    UiFeedback.loading(status, "Loading settings…");
    tasks.run(() -> new Data(account.load(), workspace.notifications(role)), data -> {
      var profile = data.profile();
      name.setText(profile.user().name());
      identifier.setText(profile.user().identifier());
      notificationsEnabled.setSelected(profile.user().notificationsEnabled());
      academicDetails.getChildren().clear();
      if (profile.student() != null) {
        var student = profile.student();
        academicDetails.getChildren().addAll(
          detail("Enrollment number", student.enrollmentNumber()),
          detail("Program", student.program()),
          detail("Academic year", student.academicYear()),
          detail("Semester", student.semester()));
        if (student.synthetic()) {
          var synthetic = new Label("Synthetic local demo record");
          synthetic.getStyleClass().add("status-draft");
          academicDetails.getChildren().add(synthetic);
        }
      }
      showNotifications(data.notifications());
      loaded = true;
      setBusy(false);
      UiFeedback.clear(status);
    }, error -> UiFeedback.error(status, error));
  }

  private void showNotifications(List<NotificationItem> items) {
    notices.getChildren().clear();
    if (items.isEmpty()) {
      notices.getChildren().add(UiFeedback.empty("You're all caught up."));
      return;
    }
    for (var note : items) {
      var title = new Label(note.title());
      title.setWrapText(true);
      HBox.setHgrow(title, Priority.ALWAYS);
      var mark = new Button(note.read() ? "Read" : "Mark read");
      mark.getStyleClass().add("secondary-button");
      mark.setDisable(note.read());
      mark.setOnAction(e -> {
        mark.setDisable(true);
        tasks.run(() -> { workspace.markRead(role, note.id()); return null; }, ignored -> {
          mark.setText("Read");
          UiFeedback.success(status, "Notification marked read.");
        }, error -> { mark.setDisable(false); UiFeedback.error(status, error); });
      });
      var row = new HBox(12, title, mark);
      row.getStyleClass().add("notification-row");
      notices.getChildren().add(row);
    }
  }

  private void save() {
    if (busy || !loaded) return;
    String displayName = name.getText().trim();
    if (displayName.isEmpty() || displayName.length() > 100) {
      UiFeedback.error(status, "Enter a full name of at most 100 characters.");
      return;
    }
    setBusy(true);
    UiFeedback.loading(status, "Saving profile…");
    tasks.run(() -> account.updateProfile(displayName, notificationsEnabled.isSelected()), updated -> {
      setBusy(false);
      name.setText(updated.name());
      userChanged.accept(updated);
      UiFeedback.success(status, "Profile updated.");
    }, error -> { setBusy(false); UiFeedback.error(status, error); });
  }

  private void changePassword() {
    if (busy || !loaded) return;
    String current = currentPassword.getText();
    String next = newPassword.getText();
    if (current.isBlank() || current.length() > 128 || next.length() < 12 || next.length() > 128) {
      UiFeedback.error(status, "Enter your current password and a new password of 12–128 characters.");
      return;
    }
    setBusy(true);
    UiFeedback.loading(status, "Updating password…");
    tasks.run(() -> { account.changePassword(current, next); return null; }, ignored -> {
      currentPassword.clear();
      newPassword.clear();
      passwordChanged.run();
    }, error -> { setBusy(false); UiFeedback.error(status, error); });
  }

  private void setBusy(boolean value) {
    busy = value;
    save.setDisable(value || !loaded);
    change.setDisable(value || !loaded);
  }

  private static HBox detail(String label, Object value) {
    var name = new Label(label);
    name.getStyleClass().add("muted");
    name.setMinWidth(135);
    var field = new Label(value == null ? "Not available" : value.toString());
    field.setWrapText(true);
    return new HBox(10, name, field);
  }

  private record Data(AccountApi.Profile profile, List<NotificationItem> notifications) { }
}
