package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.AuthContracts.UserRole;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public final class AuthView {
  private AuthView() { }

  public static Node roleSelection(Consumer<UserRole> choose) {
    var card = new VBox(18);
    card.getStyleClass().add("role-card");
    card.setAlignment(Pos.CENTER);
    card.setMaxHeight(570);
    card.getChildren().addAll(logo(178), text("ONE CAMPUS. CONNECTED.", "eyebrow"),
      text("Academic Management System", "role-title"),
      text("Your academic life, thoughtfully connected.", "muted"),
      text("Choose your portal to get started.", "muted"));
    var options = new HBox(16);
    options.setAlignment(Pos.CENTER);
    options.getChildren().addAll(portal("Faculty Portal", "Teach, guide, and inspire.", () -> choose.accept(UserRole.FACULTY)),
      portal("Student Portal", "Your learning journey starts here.", () -> choose.accept(UserRole.STUDENT)));
    card.getChildren().addAll(options, text("Thapar Institute of Engineering & Technology · Patiala", "muted"));
    var root = new StackPane(card);
    root.getStyleClass().add("auth-background");
    return root;
  }

  public static LoginPane login(UserRole role, Runnable back, LoginAction submit) {
    return new LoginPane(role, back, submit);
  }

  public interface LoginAction {
    void submit(String identifier, String password, boolean rememberMe);
  }

  public static final class LoginPane extends HBox {
    private final Button signIn = new Button("Sign In  →");
    private final Label error = text("", "error-text");
    private final Label notice = text("", "feedback");

    private LoginPane(UserRole role, Runnable back, LoginAction submit) {
      getStyleClass().add("login-page");
      var footer = text("A legacy of excellence.\nA future of possibilities.", "story-footer");
      footer.setWrapText(true);
      var story = new VBox(22, logo(165), text("Academic Management System", "story-kicker"),
        text(role == UserRole.FACULTY ? "Faculty Portal" : "Student Portal", "story-title"),
        footer);
      story.getStyleClass().add("login-story");
      VBox.setVgrow(story.getChildren().get(3), Priority.ALWAYS);

      var side = new StackPane();
      side.getStyleClass().add("login-side");
      var card = new VBox(12);
      card.getStyleClass().add("login-card");
      var backButton = new Button("←  Choose another portal");
      backButton.getStyleClass().add("link-button");
      backButton.setOnAction(e -> back.run());
      var intro = text("Welcome Back, " + (role == UserRole.FACULTY ? "Faculty" : "Student"), "login-title");
      var identifier = new TextField();
      identifier.setPromptText(role == UserRole.FACULTY ? "Employee ID or Official Email" : "Enrollment Number or Official Email");
      var password = new PasswordField();
      password.setPromptText("Enter your password");
      var remember = new CheckBox("Longer session (8 hours)");
      var forgot = new Button("Forgot Password?");
      forgot.getStyleClass().add("link-button");
      forgot.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
        "Contact your department or institute IT support to verify your identity and request a password reset.", ButtonType.OK).showAndWait());
      var options = new HBox(12, remember, forgot);
      options.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(remember, Priority.ALWAYS);
      signIn.getStyleClass().add("primary-button");
      signIn.setMaxWidth(Double.MAX_VALUE);
      signIn.setOnAction(e -> {
        if (identifier.getText().isBlank() || password.getText().isBlank()) {
          setError("Enter your identifier and password.");
          return;
        }
        submit.submit(identifier.getText().trim(), password.getText(), remember.isSelected());
      });
      password.setOnAction(e -> signIn.fire());
      error.setWrapText(true);
      notice.setWrapText(true);
      card.getChildren().addAll(backButton, text("YOUR CAMPUS WORKSPACE", "eyebrow"), intro,
        text("Sign in to your academic account.", "muted"),
        text(role == UserRole.FACULTY ? "Employee ID or Official Email" : "Enrollment Number or Official Email", "field-label"),
        identifier, text("Password", "field-label"), password, options, notice, error, signIn);
      side.getChildren().add(card);
      getChildren().addAll(story, side);
      HBox.setHgrow(side, Priority.ALWAYS);
    }

    public void setBusy(boolean busy) {
      signIn.setDisable(busy);
      signIn.setText(busy ? "Signing in…" : "Sign In  →");
    }

    public void setError(String message) { error.setText(message); }
    public void setNotice(String message) { notice.setText(message); }
  }

  private static Button portal(String title, String subtitle, Runnable action) {
    var button = new Button(title + "\n" + subtitle + "    →");
    button.getStyleClass().add("portal-button");
    button.setOnAction(e -> action.run());
    return button;
  }

  static Label text(String value, String style) {
    var label = new Label(value);
    label.getStyleClass().add(style);
    return label;
  }

  static ImageView logo(double width) {
    var image = new ImageView(new Image(AuthView.class.getResourceAsStream("/images/thapar-logo-dark.png")));
    image.setFitWidth(width);
    image.setPreserveRatio(true);
    return image;
  }
}
