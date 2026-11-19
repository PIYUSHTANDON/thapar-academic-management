package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.contract.CourseContracts.ClassSessionItem;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.navigation.Screen;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public final class DashboardUi {
  private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private DashboardUi() { }

  public static StackPane hero(UserResponse user, String subtitle, List<Stat> stats,
                               List<NotificationItem> notices, WorkspaceApi api, TaskRunner tasks,
                               Label message) {
    var hero = new StackPane();
    hero.getStyleClass().add("dashboard-hero");
    var photo = new ImageView(new Image(DashboardUi.class.getResourceAsStream("/images/campus.jpg")));
    photo.setManaged(false);
    photo.setPreserveRatio(false);
    photo.fitWidthProperty().bind(hero.widthProperty());
    photo.fitHeightProperty().bind(hero.heightProperty());
    var shade = new Region();
    shade.getStyleClass().add("hero-shade");
    var noticeRow = new HBox(notifications(user, notices, api, tasks, message));
    noticeRow.setAlignment(Pos.TOP_RIGHT);
    var welcome = new Label("Welcome Back, " + user.name() + "!");
    welcome.getStyleClass().add("hero-title");
    welcome.setWrapText(true);
    welcome.setMinWidth(0);
    welcome.setMaxWidth(Double.MAX_VALUE);
    var copy = new VBox(7, text("Thapar Institute", "hero-kicker"), welcome,
      text(subtitle, "hero-subtitle"));
    var space = new Region();
    VBox.setVgrow(space, Priority.ALWAYS);
    var cards = new HBox(12);
    for (var stat : stats) {
      var value = text(stat.value(), "stat-number");
      value.setWrapText(true);
      var label = text(stat.label(), "stat-title");
      label.setWrapText(true);
      var card = new VBox(6, value, label);
      card.getStyleClass().add("stat-card");
      card.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(card, Priority.ALWAYS);
      cards.getChildren().add(card);
    }
    var content = new VBox(10, noticeRow, copy, space, cards);
    content.getStyleClass().add("hero-content");
    hero.getChildren().addAll(photo, shade, content);
    return hero;
  }

  private static MenuButton notifications(UserResponse user, List<NotificationItem> notices, WorkspaceApi api,
                                          TaskRunner tasks, Label message) {
    int count = (int) notices.stream().filter(note -> !note.read()).count();
    var unread = new AtomicInteger(count);
    var menu = new MenuButton("Notifications" + (count == 0 ? "" : " (" + count + ")"));
    menu.getStyleClass().add("hero-notifications");
    if (notices.isEmpty()) {
      var empty = new MenuItem("You're all caught up.");
      empty.setDisable(true);
      menu.getItems().add(empty);
    }
    String role = user.role() == UserRole.FACULTY ? "faculty" : "student";
    for (var note : notices) {
      var item = new MenuItem((note.read() ? "✓ " : "● ") + note.title());
      item.setDisable(note.read());
      item.setOnAction(e -> tasks.run(() -> { api.markRead(role, note.id()); return null; }, ignored -> {
        item.setText("✓ " + note.title());
        item.setDisable(true);
        int left = unread.decrementAndGet();
        menu.setText("Notifications" + (left == 0 ? "" : " (" + left + ")"));
      }, error -> message.setText(error.getMessage())));
      menu.getItems().add(item);
    }
    return menu;
  }

  public static VBox panel(String heading, Node... body) {
    var title = text(heading, "panel-title");
    var panel = new VBox(15, title);
    panel.getChildren().addAll(body);
    panel.getStyleClass().add("panel");
    return panel;
  }

  public static Button action(String title, Screen destination, Consumer<Screen> navigate) {
    var button = new Button(title.replace(' ', '\n') + " →");
    button.getStyleClass().add("quick-action");
    button.setWrapText(true);
    button.setPrefHeight(72);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setOnAction(e -> navigate.accept(destination));
    return button;
  }

  public static TilePane actions(Button... buttons) {
    var tiles = new TilePane(10, 10);
    tiles.setPrefTileWidth(145);
    tiles.setPrefColumns(buttons.length);
    tiles.getChildren().addAll(buttons);
    return tiles;
  }

  public static VBox today(List<ClassSessionItem> schedule, String empty) {
    var rows = new VBox();
    rows.getChildren().add(scheduleRow("Time", "Course", "Room", "Status", true));
    var current = LocalDate.now(CAMPUS);
    var today = schedule.stream().filter(item -> current.equals(item.startsAt().toLocalDate())).toList();
    if (today.isEmpty()) rows.getChildren().add(text(empty, "empty-state"));
    for (var item : today) {
      String time = item.startsAt().format(TIME) + " – " + item.endsAt().format(TIME);
      rows.getChildren().add(scheduleRow(time, item.course(), item.room(), item.status(), false));
    }
    return rows;
  }

  private static GridPane scheduleRow(String time, String course, String room, String status, boolean heading) {
    var row = new GridPane();
    row.setHgap(8);
    row.getStyleClass().add(heading ? "table-heading" : "schedule-row");
    double[] widths = {23, 37, 20, 20};
    String[] values = {time, course, room, status};
    for (int i = 0; i < values.length; i++) {
      var column = new ColumnConstraints();
      column.setPercentWidth(widths[i]);
      row.getColumnConstraints().add(column);
      var label = new Label(values[i]);
      label.setWrapText(true);
      label.setMaxWidth(Double.MAX_VALUE);
      row.add(label, i, 0);
    }
    return row;
  }

  public static Label text(String value, String style) {
    var label = new Label(value);
    label.getStyleClass().add(style);
    return label;
  }

  public record Stat(String label, String value) { }
}
