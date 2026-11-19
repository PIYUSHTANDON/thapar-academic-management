package com.thapar.ams.desktop.ui;

import javafx.scene.control.Label;

public final class UiFeedback {
  private UiFeedback() { }

  public static void loading(Label label, String text) { show(label, text, false); }
  public static void success(Label label, String text) { show(label, text, false); }
  public static void clear(Label label) { show(label, "", false); }
  public static void error(Label label, Throwable cause) {
    error(label, cause.getMessage() == null ? "Something went wrong. Please retry." : cause.getMessage());
  }
  public static void error(Label label, String text) { show(label, text, true); }

  public static Label empty(String text) {
    var label = new Label(text);
    label.getStyleClass().add("empty-state");
    label.setWrapText(true);
    return label;
  }

  private static void show(Label label, String text, boolean error) {
    label.setText(text);
    label.getStyleClass().remove("error-text");
    if (error) label.getStyleClass().add("error-text");
  }
}
