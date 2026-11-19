package com.thapar.ams.desktop.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public final class CourseUi {
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
  private CourseUi() { }

  public static VBox page(String heading, String description, Label message, Node... content) {
    var title = new Label(heading);
    title.getStyleClass().add("module-title");
    var subtitle = new Label(description);
    subtitle.getStyleClass().add("muted");
    message.getStyleClass().add("feedback");
    message.setWrapText(true);
    var page = new VBox(20, title, subtitle, message);
    page.getChildren().addAll(content);
    page.getStyleClass().add("module-page");
    return page;
  }

  public static void fill(ScrollPane pane, VBox page) {
    pane.setContent(page);
    pane.setFitToWidth(true);
    pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    pane.getStyleClass().add("page-scroll");
  }

  public static VBox panel(String heading, Node... content) {
    var title = new Label(heading);
    title.getStyleClass().add("panel-title");
    var panel = new VBox(16, title);
    panel.getChildren().addAll(content);
    panel.getStyleClass().add("panel");
    return panel;
  }

  public static Label label(String value) {
    var label = new Label(value);
    label.getStyleClass().add("field-label");
    return label;
  }

  public static String date(LocalDateTime value) {
    return value == null ? "Date unavailable" : value.format(DATE);
  }

  public static <T> void formatChoices(ComboBox<T> box, Function<T, String> renderer) {
    box.setConverter(new StringConverter<>() {
      @Override public String toString(T value) { return value == null ? "" : renderer.apply(value); }
      @Override public T fromString(String text) { return null; }
    });
  }
}
