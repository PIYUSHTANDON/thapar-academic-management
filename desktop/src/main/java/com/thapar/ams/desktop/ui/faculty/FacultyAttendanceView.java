package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import com.thapar.ams.contract.AttendanceContracts.FacultyAttendanceRecord;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.desktop.api.AttendanceApi;
import com.thapar.ams.desktop.qr.QrCodec;
import com.thapar.ams.desktop.ui.CourseUi;
import com.thapar.ams.desktop.ui.TaskRunner;
import com.thapar.ams.desktop.ui.UiFeedback;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public final class FacultyAttendanceView extends ScrollPane implements AutoCloseable {
  private static final DateTimeFormatter RECORD_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
  private final AttendanceApi api;
  private final TaskRunner tasks;
  private final boolean reportsOnly;
  private final ComboBox<ClassSessionItem> sessions = new ComboBox<>();
  private final Button generate = new Button("Generate attendance QR");
  private final Label message = new Label();
  private final Label countdown = new Label();
  private final ImageView qr = new ImageView();
  private final VBox records = new VBox();
  private Timeline timer;
  private AttendanceToken token;

  public FacultyAttendanceView(AttendanceApi api, TaskRunner tasks, boolean reportsOnly) {
    this.api = api;
    this.tasks = tasks;
    this.reportsOnly = reportsOnly;
    var page = new VBox(20);
    page.getStyleClass().add("module-page");
    page.getChildren().addAll(title(reportsOnly ? "Attendance Reports" : "Attendance",
      reportsOnly ? "Review recorded attendance for your classes." :
        "Manage secure attendance for your teaching sessions."), message);
    message.getStyleClass().add("feedback");
    message.setWrapText(true);
    if (!reportsOnly) page.getChildren().add(openPanel());
    var recordPanel = panel("Attendance records");
    var refresh = new Button("Refresh records");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    recordPanel.getChildren().add(refresh);
    recordPanel.getChildren().add(records);
    page.getChildren().add(recordPanel);
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load();
  }

  private VBox openPanel() {
    var box = panel("Open attendance session");
    box.getChildren().add(new Label("Each QR expires after 3 minutes. The server checks student identity and enrollment."));
    sessions.setMaxWidth(Double.MAX_VALUE);
    CourseUi.formatChoices(sessions, item -> item.course() + " · " + item.startsAt().toString().replace('T', ' ') + " · " + item.status());
    HBox.setHgrow(sessions, Priority.ALWAYS);
    generate.getStyleClass().add("primary-button");
    generate.setMinWidth(220);
    generate.setDisable(true);
    generate.setOnAction(e -> open());
    box.getChildren().add(new HBox(12, sessions, generate));
    qr.setFitWidth(240);
    qr.setFitHeight(240);
    qr.setVisible(false);
    qr.setManaged(false);
    countdown.getStyleClass().add("feedback");
    box.getChildren().addAll(countdown, qr);
    return box;
  }

  private void load() {
    UiFeedback.loading(message, "Loading attendance…");
    tasks.run(() -> new State(reportsOnly ? List.of() : api.sessions(), api.facultyRecords()), state -> {
      if (!reportsOnly) {
        sessions.getItems().setAll(state.sessions());
        if (!state.sessions().isEmpty()) sessions.getSelectionModel().select(state.sessions().stream()
          .filter(session -> "Ongoing".equals(session.status())).findFirst().orElse(state.sessions().get(0)));
        generate.setDisable(state.sessions().isEmpty());
      }
      showRecords(state.records());
      UiFeedback.clear(message);
    }, error -> UiFeedback.error(message, error));
  }

  private void open() {
    ClassSessionItem selected = sessions.getValue();
    if (selected == null) return;
    generate.setDisable(true);
    UiFeedback.loading(message, "Opening attendance session…");
    tasks.run(() -> api.open(selected.id()), issued -> {
      try {
        token = issued;
        qr.setImage(QrCodec.encode(issued));
        qr.setVisible(true);
        qr.setManaged(true);
        UiFeedback.success(message, "Attendance QR is ready for session #" + issued.sessionId() + ".");
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> tick()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        tick();
      } catch (RuntimeException e) {
        UiFeedback.error(message, e);
      } finally {
        generate.setDisable(false);
      }
    }, error -> {
      generate.setDisable(false);
      UiFeedback.error(message, error);
    });
  }

  private void tick() {
    if (token == null) return;
    long seconds = Math.max(0, Duration.between(java.time.Instant.now(),
      token.expiresAt().atZone(ZoneId.of("Asia/Kolkata")).toInstant()).toSeconds());
    countdown.setText(seconds == 0 ? "Expired — generate a new token" : "Active · " + seconds + "s remaining");
    if (seconds == 0) {
      qr.setVisible(false);
      qr.setManaged(false);
      timer.stop();
    }
  }

  private void showRecords(List<FacultyAttendanceRecord> items) {
    records.getChildren().clear();
    records.getChildren().add(row("Student", "Course", "Session", "Recorded", true));
    if (items.isEmpty()) records.getChildren().add(UiFeedback.empty("No attendance recorded yet."));
    for (var item : items) records.getChildren().add(row(item.student(), item.course(),
      "#" + item.sessionId(), item.recordedAt().format(RECORD_TIME), false));
  }

  private static HBox row(String a, String b, String c, String d, boolean heading) {
    var row = new HBox(12);
    row.getStyleClass().add(heading ? "attendance-heading" : "attendance-row");
    for (String value : new String[] {a, b, c, d}) {
      var label = new Label(value);
      label.setWrapText(true);
      label.setMinWidth(0);
      label.setPrefWidth(145);
      label.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(label, Priority.ALWAYS);
      row.getChildren().add(label);
    }
    return row;
  }

  private static VBox panel(String heading) {
    var box = new VBox(17, title(heading, null));
    box.getStyleClass().add("panel");
    return box;
  }

  private static VBox title(String heading, String subtitle) {
    var title = new Label(heading);
    title.getStyleClass().add("module-title");
    var box = new VBox(7, title);
    if (subtitle != null) {
      var detail = new Label(subtitle);
      detail.getStyleClass().add("muted");
      box.getChildren().add(detail);
    }
    return box;
  }

  @Override public void close() { if (timer != null) timer.stop(); }

  private record State(List<ClassSessionItem> sessions, List<FacultyAttendanceRecord> records) { }
}
