package com.thapar.ams.desktop;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.desktop.api.AccountApi;
import com.thapar.ams.desktop.api.ApiClient;
import com.thapar.ams.desktop.api.AssessmentApi;
import com.thapar.ams.desktop.api.AttendanceApi;
import com.thapar.ams.desktop.api.CourseApi;
import com.thapar.ams.desktop.api.MessageApi;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.navigation.Screen;
import com.thapar.ams.desktop.ui.AppShell;
import com.thapar.ams.desktop.ui.AuthView;
import com.thapar.ams.desktop.ui.CalendarView;
import com.thapar.ams.desktop.ui.faculty.FacultyAssessmentsView;
import com.thapar.ams.desktop.ui.faculty.FacultyAttendanceView;
import com.thapar.ams.desktop.ui.faculty.FacultyCoursesView;
import com.thapar.ams.desktop.ui.faculty.FacultyDashboardView;
import com.thapar.ams.desktop.ui.faculty.FacultyMessagesView;
import com.thapar.ams.desktop.ui.faculty.FacultyRosterView;
import com.thapar.ams.desktop.ui.faculty.FacultySettingsView;
import com.thapar.ams.desktop.ui.student.StudentAssessmentsView;
import com.thapar.ams.desktop.ui.student.StudentAttendanceView;
import com.thapar.ams.desktop.ui.student.StudentCoursesView;
import com.thapar.ams.desktop.ui.student.StudentDashboardView;
import com.thapar.ams.desktop.ui.student.StudentMessagesView;
import com.thapar.ams.desktop.ui.student.StudentResultsView;
import com.thapar.ams.desktop.ui.student.StudentSettingsView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class App extends Application {
  private final ApiClient api = new ApiClient();
  private final ExecutorService background = Executors.newVirtualThreadPerTaskExecutor();
  private Stage stage;
  private Scene scene;
  private UserResponse user;
  private Screen current;
  private Node currentContent;
  private boolean sidebarExpanded = true;

  public static void main(String[] args) { launch(args); }

  @Override public void start(Stage stage) {
    this.stage = stage;
    scene = new Scene(new StackPane(new Label("Opening your academic workspace…")), 1366, 768);
    scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
      () -> { if (user != null) navigate(Screen.home(user.role())); });
    scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
      () -> { if (user != null) navigate(user.role() == UserRole.FACULTY ? Screen.FACULTY_MESSAGES : Screen.STUDENT_MESSAGES); });
    scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
      () -> { if (user != null) navigate(user.role() == UserRole.FACULTY ? Screen.FACULTY_SETTINGS : Screen.STUDENT_SETTINGS); });
    scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
    stage.setTitle("Thapar | Academic Management System");
    stage.setMinWidth(1000);
    stage.setMinHeight(680);
    stage.setScene(scene);
    stage.show();
    run(api::session, restored -> {
      if (restored == null || restored.role() == UserRole.ADMIN) showRoles();
      else { user = restored; navigate(Screen.home(restored.role())); }
    }, error -> showRoles());
  }

  @Override public void stop() {
    closeCurrent();
    background.shutdownNow();
  }

  private void closeCurrent() {
    if (currentContent instanceof AutoCloseable closing) {
      try { closing.close(); } catch (Exception ignored) { }
    }
    currentContent = null;
  }

  private void showRoles() {
    closeCurrent();
    user = null;
    current = null;
    scene.setRoot((javafx.scene.Parent) AuthView.roleSelection(this::showLogin));
  }

  private void showLogin(UserRole role) {
    closeCurrent();
    var view = AuthView.login(role, this::showRoles, (identifier, password, remember) -> {
      var loginView = (AuthView.LoginPane) scene.getRoot();
      loginView.setBusy(true);
      loginView.setError("");
      run(() -> api.login(identifier, password, role, remember), signedIn -> {
        user = signedIn;
        navigate(Screen.home(signedIn.role()));
      }, error -> {
        loginView.setBusy(false);
        loginView.setError(error instanceof ApiClient.ApiException apiError && apiError.status() == 401
          ? "The credentials or selected role are incorrect." : message(error));
      });
    });
    scene.setRoot(view);
  }

  private void navigate(Screen destination) {
    if (user == null || destination.role() != user.role()) return;
    closeCurrent();
    current = destination;
    if (destination == Screen.FACULTY_DASHBOARD) {
      render(destination, new FacultyDashboardView(new WorkspaceApi(api), this::run, user, this::navigate));
    } else if (destination == Screen.STUDENT_DASHBOARD) {
      render(destination, new StudentDashboardView(new WorkspaceApi(api), this::run, user, this::navigate));
    } else if (destination == Screen.FACULTY_ATTENDANCE || destination == Screen.FACULTY_REPORTS) {
      render(destination, new FacultyAttendanceView(new AttendanceApi(api), this::run,
        destination == Screen.FACULTY_REPORTS));
    } else if (destination == Screen.STUDENT_ATTENDANCE) {
      render(destination, new StudentAttendanceView(new AttendanceApi(api), this::run));
    } else if (destination == Screen.FACULTY_ASSESSMENTS) {
      render(destination, new FacultyAssessmentsView(new AssessmentApi(api), this::run));
    } else if (destination == Screen.STUDENT_ASSESSMENTS) {
      render(destination, new StudentAssessmentsView(new AssessmentApi(api), this::run));
    } else if (destination == Screen.STUDENT_RESULTS) {
      render(destination, new StudentResultsView(new AssessmentApi(api), this::run));
    } else if (destination == Screen.FACULTY_COURSES) {
      render(destination, new FacultyCoursesView(new CourseApi(api), this::run));
    } else if (destination == Screen.STUDENT_COURSES) {
      render(destination, new StudentCoursesView(new CourseApi(api), this::run));
    } else if (destination == Screen.FACULTY_STUDENTS) {
      render(destination, new FacultyRosterView(new CourseApi(api), this::run));
    } else if (destination == Screen.FACULTY_CALENDAR || destination == Screen.STUDENT_CALENDAR) {
      render(destination, new CalendarView(new WorkspaceApi(api), new CourseApi(api), this::run,
        destination == Screen.FACULTY_CALENDAR));
    } else if (destination == Screen.FACULTY_MESSAGES) {
      render(destination, new FacultyMessagesView(new MessageApi(api, UserRole.FACULTY), this::run, user));
    } else if (destination == Screen.STUDENT_MESSAGES) {
      render(destination, new StudentMessagesView(new MessageApi(api, UserRole.STUDENT), this::run, user));
    } else if (destination == Screen.FACULTY_SETTINGS) {
      render(destination, new FacultySettingsView(new AccountApi(api, UserRole.FACULTY), new WorkspaceApi(api),
        this::run, user, this::userChanged, this::passwordChanged, this::logout));
    } else if (destination == Screen.STUDENT_SETTINGS) {
      render(destination, new StudentSettingsView(new AccountApi(api, UserRole.STUDENT), new WorkspaceApi(api),
        this::run, user, this::userChanged, this::passwordChanged, this::logout));
    } else {
      throw new IllegalStateException("Unmapped screen: " + destination);
    }
  }

  private void userChanged(UserResponse updated) {
    user = updated;
    render(current, currentContent);
  }

  private void passwordChanged() {
    UserRole role = user.role();
    closeCurrent();
    user = null;
    current = null;
    showLogin(role);
    ((AuthView.LoginPane) scene.getRoot()).setNotice("Password changed. Sign in with your new password.");
  }

  private void render(Screen destination, Node content) {
    currentContent = content;
    scene.setRoot(AppShell.build(user, destination, content, sidebarExpanded, this::navigate,
      expanded -> { sidebarExpanded = expanded; render(current, currentContent); }, this::logout));
  }

  private void logout() {
    closeCurrent();
    run(() -> { api.logout(); return null; }, ignored -> showRoles(),
      error -> render(current, errorView(error, this::logout)));
  }

  private Node errorView(Throwable error, Runnable retry) {
    var button = new Button("Retry");
    button.getStyleClass().add("primary-button");
    button.setOnAction(e -> retry.run());
    var content = new VBox(14, new Label(message(error)), button);
    content.getStyleClass().add("placeholder");
    return content;
  }

  private String message(Throwable error) {
    if (error instanceof ApiClient.ApiException apiError) {
      return switch (apiError.status()) {
        case 401 -> "Your session has ended. Sign in again.";
        case 403 -> "You do not have access to this section.";
        case 429 -> "Too many sign-in attempts. Please wait 15 minutes.";
        default -> apiError.getMessage();
      };
    }
    return error.getMessage() == null ? "Something went wrong." : error.getMessage();
  }

  private <T> void run(java.util.concurrent.Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
    background.submit(() -> {
      try {
        T result = work.call();
        Platform.runLater(() -> success.accept(result));
      } catch (Exception e) {
        Platform.runLater(() -> {
          if (e instanceof ApiClient.ApiException apiError && apiError.status() == 401 && user != null) {
            UserRole role = user.role();
            closeCurrent();
            user = null;
            current = null;
            showLogin(role);
          } else failure.accept(e);
        });
      }
    });
  }
}
