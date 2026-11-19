package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.desktop.navigation.Screen;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public final class AppShell {
  private AppShell() { }

  public static BorderPane build(UserResponse user, Screen current, Node content, boolean expanded,
      Consumer<Screen> navigate, Consumer<Boolean> toggle, Runnable logout) {
    var shell = new BorderPane();
    shell.getStyleClass().add("app-shell");
    var sidebar = new VBox(9);
    sidebar.getStyleClass().add("sidebar");
    sidebar.getStyleClass().add(expanded ? "expanded" : "collapsed");
    sidebar.setPrefWidth(expanded ? 238 : 72);
    sidebar.setMinWidth(expanded ? 238 : 72);
    var logo = new ImageView(new Image(AppShell.class.getResourceAsStream(
      expanded ? "/images/thapar-logo-dark.png" : "/images/thapar-crest-dark.png")));
    logo.setPreserveRatio(true);
    logo.setFitWidth(expanded ? 165 : 38);
    var brand = new StackPane(logo);
    brand.getStyleClass().add("sidebar-brand");
    var toggleButton = new Button(expanded ? "☰  Collapse" : "☰");
    toggleButton.getStyleClass().add("sidebar-toggle");
    toggleButton.setOnAction(e -> toggle.accept(!expanded));
    sidebar.getChildren().addAll(brand, toggleButton);
    for (Screen screen : Screen.navigation(user.role())) {
      var button = new Button(expanded ? screen.title() : icon(screen));
      button.setMaxWidth(Double.MAX_VALUE);
      button.getStyleClass().add("sidebar-link");
      if (screen == current) button.getStyleClass().add("selected");
      button.setTooltip(new Tooltip(screen.title() + shortcut(screen)));
      button.setOnAction(e -> navigate.accept(screen));
      sidebar.getChildren().add(button);
    }
    var spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    var profile = new Button(expanded ? user.name() + "\n" + user.identifier() : initials(user.name()));
    profile.getStyleClass().add("profile-button");
    profile.setMaxWidth(Double.MAX_VALUE);
    profile.setTooltip(new Tooltip(user.name() + " · Profile & settings (Ctrl+,)"));
    var menu = new ContextMenu();
    var settings = new MenuItem("Profile & settings");
    settings.setOnAction(e -> navigate.accept(user.role() == UserRole.FACULTY
      ? Screen.FACULTY_SETTINGS : Screen.STUDENT_SETTINGS));
    var signout = new MenuItem("Sign out");
    signout.setOnAction(e -> logout.run());
    menu.getItems().addAll(settings, signout);
    profile.setOnAction(e -> menu.show(profile, javafx.geometry.Side.TOP, 0, 0));
    sidebar.getChildren().addAll(spacer, profile);
    shell.setLeft(sidebar);
    shell.setCenter(content);
    return shell;
  }

  private static String initials(String name) {
    return name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
  }

  private static String shortcut(Screen screen) {
    return switch (screen) {
      case FACULTY_DASHBOARD, STUDENT_DASHBOARD -> " (Ctrl+1)";
      case FACULTY_MESSAGES, STUDENT_MESSAGES -> " (Ctrl+2)";
      case FACULTY_SETTINGS, STUDENT_SETTINGS -> " (Ctrl+,)";
      default -> "";
    };
  }

  private static String icon(Screen screen) {
    return switch (screen.title()) {
      case "Dashboard" -> "⌂";
      case "Messages" -> "✉";
      case "Attendance" -> "✓";
      case "Courses & LMS" -> "▤";
      case "Assessments" -> "✎";
      case "Calendar" -> "▦";
      case "Settings" -> "⚙";
      default -> "◉";
    };
  }
}
