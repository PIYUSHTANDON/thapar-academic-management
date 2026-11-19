package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceSummary;
import com.thapar.ams.desktop.api.AttendanceApi;
import com.thapar.ams.desktop.qr.QrCodec;
import com.thapar.ams.desktop.qr.QrScanner;
import com.thapar.ams.desktop.qr.WebcamQrScanner;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.io.File;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class StudentAttendanceView extends ScrollPane implements AutoCloseable {
  private static final DateTimeFormatter CLASS_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
  private final AttendanceApi api;
  private final TaskRunner tasks;
  private final TextArea tokenInput = new TextArea();
  private final Button claim = new Button("Claim attendance");
  private final Label message = new Label();
  private final VBox summary = new VBox(14);
  private final VBox history = new VBox(12);
  private QrScanner scanner;

  public StudentAttendanceView(AttendanceApi api, TaskRunner tasks) {
    this.api = api;
    this.tasks = tasks;
    var page = new VBox(20);
    page.getStyleClass().add("module-page");
    var title = new Label("Attendance");
    title.getStyleClass().add("module-title");
    var subtitle = new Label("Claim class attendance and review your records.");
    subtitle.getStyleClass().add("muted");
    page.getChildren().addAll(title, subtitle, message, claimPanel(), panel("Subject-wise attendance", summary),
      panel("Class attendance history", history));
    var refresh = new Button("Refresh attendance");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load());
    page.getChildren().add(3, refresh);
    message.getStyleClass().add("feedback");
    message.setWrapText(true);
    setContent(page);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    getStyleClass().add("page-scroll");
    load();
  }

  private VBox claimPanel() {
    var box = new VBox(15);
    box.getStyleClass().add("panel");
    var heading = new Label("Claim class attendance");
    heading.getStyleClass().add("panel-title");
    var explanation = new Label("Scan the faculty QR, upload an image, or enter its short-lived token. Your identity and enrollment are checked by the server.");
    explanation.setWrapText(true);
    var camera = new Button("Scan with camera");
    var upload = new Button("Upload QR image");
    camera.getStyleClass().add("secondary-button");
    upload.getStyleClass().add("secondary-button");
    camera.setOnAction(e -> openCamera());
    upload.setOnAction(e -> chooseImage());
    tokenInput.setPromptText("Paste the token or QR JSON content");
    tokenInput.setPrefRowCount(2);
    claim.getStyleClass().add("primary-button");
    claim.setOnAction(e -> claim());
    box.getChildren().addAll(heading, explanation, new HBox(12, camera, upload),
      new Label("Attendance token"), tokenInput, claim);
    return box;
  }

  private void load() {
    message.setText("Loading attendance…");
    tasks.run(api::studentSummary, this::showSummary, error -> message.setText(error.getMessage()));
  }

  private void showSummary(StudentAttendanceSummary data) {
    summary.getChildren().clear();
    history.getChildren().clear();
    summary.getChildren().add(new Label("Overall: " + percentage(data.percentage()) +
      "   ·   Attended: " + data.attended() + " / " + data.total()));
    var scope = new Label("Percentages include completed classes; ongoing claims appear in history immediately.");
    scope.getStyleClass().add("muted");
    scope.setWrapText(true);
    summary.getChildren().add(scope);
    if (data.courses().isEmpty()) summary.getChildren().add(new Label("No enrolled courses."));
    for (var course : data.courses()) summary.getChildren().add(row(
      course.course() + " · " + course.section(),
      "Attended " + course.attended() + "  ·  Missed " + course.missed() +
      "  ·  Total " + course.total() + "  ·  " + percentage(course.percentage())));
    if (data.records().isEmpty()) history.getChildren().add(new Label("No completed classes to report yet."));
    for (var record : data.records()) history.getChildren().add(row(
      record.course() + " · " + record.room(),
      record.startsAt().format(CLASS_TIME) + "  ·  " + record.status()));
    message.setText("");
  }

  private void claim() {
    String token;
    try { token = QrCodec.parseToken(tokenInput.getText()); }
    catch (IllegalArgumentException e) { message.setText(e.getMessage()); return; }
    claim.setDisable(true);
    message.setText("Claiming attendance…");
    tasks.run(() -> { api.claim(token); return api.studentSummary(); }, data -> {
      claim.setDisable(false);
      tokenInput.clear();
      showSummary(data);
      message.setText("Attendance recorded.");
    }, error -> {
      claim.setDisable(false);
      message.setText(error.getMessage());
    });
  }

  private void chooseImage() {
    var chooser = new FileChooser();
    chooser.setTitle("Choose attendance QR image");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("QR images", "*.png", "*.jpg", "*.jpeg"));
    File file = chooser.showOpenDialog(getScene().getWindow());
    if (file == null) return;
    if (file.length() > 5 * 1024 * 1024) {
      message.setText("Choose a QR image up to 5 MB.");
      return;
    }
    message.setText("Reading QR image…");
    tasks.run(() -> QrCodec.decode(file), token -> {
      tokenInput.setText(token);
      message.setText("QR decoded. Review and claim below.");
    }, error -> message.setText(error.getMessage()));
  }

  private void openCamera() {
    closeScanner();
    scanner = new WebcamQrScanner();
    var preview = new ImageView();
    preview.setFitWidth(480);
    preview.setFitHeight(340);
    preview.setPreserveRatio(true);
    var note = new Label("Point your camera at the faculty attendance QR.");
    var content = new VBox(12, note, preview);
    var dialog = new Dialog<Void>();
    dialog.setTitle("Scan attendance QR");
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.setOnHidden(e -> closeScanner());
    dialog.show();
    scanner.start(preview::setImage, token -> {
      tokenInput.setText(token);
      message.setText("QR decoded. Review and claim below.");
      dialog.close();
    }, error -> {
      note.setText(error.getMessage());
      closeScanner();
    });
  }

  private void closeScanner() {
    if (scanner != null) {
      scanner.close();
      scanner = null;
    }
  }

  private static String percentage(Long value) { return value == null ? "Not available" : value + "%"; }

  private static HBox row(String left, String right) {
    var row = new HBox(18, new Label(left), new Label(right));
    row.getStyleClass().add("attendance-row");
    return row;
  }

  private static VBox panel(String title, VBox content) {
    var heading = new Label(title);
    heading.getStyleClass().add("panel-title");
    var box = new VBox(16, heading, content);
    box.getStyleClass().add("panel");
    return box;
  }

  @Override public void close() { closeScanner(); }
}
