package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class CalendarWidget extends VBox {
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
  private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
  private final Label monthLabel = new Label();
  private final GridPane grid = new GridPane();
  private final VBox selectedEvents = new VBox(7);
  private List<CalendarEvent> events = List.of();
  private YearMonth month = YearMonth.now(CAMPUS);
  private LocalDate selected = LocalDate.now(CAMPUS);

  public CalendarWidget() {
    super(16);
    getStyleClass().add("panel");
    getStyleClass().add("calendar-widget");
    var previous = new Button("‹");
    previous.setAccessibleText("Previous month");
    previous.getStyleClass().add("calendar-arrow");
    previous.setOnAction(e -> move(-1));
    var next = new Button("›");
    next.setAccessibleText("Next month");
    next.getStyleClass().add("calendar-arrow");
    next.setOnAction(e -> move(1));
    monthLabel.getStyleClass().add("panel-title");
    monthLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(monthLabel, Priority.ALWAYS);
    widthProperty().addListener((changed, before, after) -> updateMonthLabel());
    var header = new HBox(12, previous, monthLabel, next);
    header.setAlignment(Pos.CENTER);
    grid.getStyleClass().add("calendar-grid");
    for (int i = 0; i < 7; i++) {
      var column = new ColumnConstraints();
      column.setPercentWidth(100.0 / 7);
      grid.getColumnConstraints().add(column);
    }
    var legend = new javafx.scene.layout.TilePane(8, 6, legend("Class", "class"),
      legend("Assignment", "assignment"), legend("Quiz", "quiz"), legend("Event", "event"));
    legend.setPrefTileWidth(96);
    legend.getStyleClass().add("calendar-legend");
    getChildren().addAll(header, grid, legend, selectedEvents);
    render();
  }

  public void setEvents(List<CalendarEvent> items) {
    events = items == null ? List.of() : items;
    render();
  }

  public void selectDate(LocalDate date) {
    selected = date;
    month = YearMonth.from(date);
    render();
  }

  private void move(int months) {
    month = month.plusMonths(months);
    selected = month.atDay(1);
    render();
  }

  private void render() {
    updateMonthLabel();
    grid.getChildren().clear();
    String[] weekdays = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
    for (int col = 0; col < 7; col++) {
      var label = new Label(weekdays[col]);
      label.getStyleClass().add("calendar-weekday");
      label.setMaxWidth(Double.MAX_VALUE);
      label.setAlignment(Pos.CENTER);
      grid.add(label, col, 0);
    }
    int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
    for (int day = 1; day <= month.lengthOfMonth(); day++) {
      LocalDate date = month.atDay(day);
      var button = new Button(Integer.toString(day));
      button.getStyleClass().add("calendar-day");
      if (date.equals(selected)) button.getStyleClass().add("selected");
      if (date.equals(LocalDate.now(CAMPUS))) button.getStyleClass().add("today");
      button.setMaxWidth(Double.MAX_VALUE);
      button.setMinHeight(38);
      var kinds = events.stream().filter(item -> date.equals(item.date()))
        .map(this::kind).distinct().toList();
      if (!kinds.isEmpty()) {
        var markers = new HBox(3);
        markers.setAlignment(Pos.CENTER);
        for (var kind : kinds) {
          var dot = new Region();
          dot.getStyleClass().addAll("calendar-dot", kind);
          markers.getChildren().add(dot);
        }
        button.setGraphic(markers);
        button.setContentDisplay(ContentDisplay.BOTTOM);
      }
      button.setAccessibleText(date + (kinds.isEmpty() ? "" : ", " + kinds.size() + " event types"));
      button.setOnAction(e -> { selected = date; render(); });
      int index = offset + day - 1;
      grid.add(button, index % 7, 1 + index / 7);
    }
    selectedEvents.getChildren().clear();
    var heading = new Label(selected.format(DAY));
    heading.getStyleClass().add("field-label");
    selectedEvents.getChildren().add(heading);
    var dayEvents = events.stream().filter(item -> selected.equals(item.date())).toList();
    if (dayEvents.isEmpty()) selectedEvents.getChildren().add(new Label("No academic events"));
    for (var item : dayEvents) {
      var title = new Label(item.title());
      title.setWrapText(true);
      var line = new HBox(8, dot(kind(item)), title);
      line.setAlignment(Pos.CENTER_LEFT);
      selectedEvents.getChildren().add(line);
    }
  }

  private void updateMonthLabel() {
    monthLabel.setText(month.format(getWidth() < 280 ? SHORT_MONTH : MONTH));
    monthLabel.setAccessibleText(month.format(MONTH));
  }

  private String kind(CalendarEvent item) {
    if (item.kind() != null) return item.kind().toLowerCase(Locale.ROOT);
    if (item.id().startsWith("class-")) return "class";
    if (item.id().startsWith("assessment-")) return "assignment";
    return "event";
  }

  private static HBox legend(String label, String kind) {
    var text = new Label(label);
    text.getStyleClass().add("muted");
    var row = new HBox(5, dot(kind), text);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private static Region dot(String kind) {
    var dot = new Region();
    dot.getStyleClass().addAll("calendar-dot", kind);
    return dot;
  }
}
