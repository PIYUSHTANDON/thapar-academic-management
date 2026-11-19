package com.thapar.ams.desktop.ui;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.MessageContracts.ContactItem;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.desktop.api.MessageApi;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MessagesView extends ScrollPane {
  private final MessageApi api;
  private final TaskRunner tasks;
  private final UserResponse user;
  private final String contactEmpty;
  private final String conversationEmpty;
  private final Label status = new Label();
  private final Label conversationTitle = new Label("Select a conversation");
  private final VBox contactRows = new VBox(7);
  private final VBox messageRows = new VBox(10);
  private final ScrollPane history = new ScrollPane(messageRows);
  private final TextField body = new TextField();
  private final Button send = new Button("Send");
  private List<ContactItem> contacts = List.of();
  private List<MessageItem> messages = List.of();
  private Long selectedId;
  private boolean busy;

  protected MessagesView(MessageApi api, TaskRunner tasks, UserResponse user, String subtitle,
                         String contactHeading, String contactEmpty, String conversationEmpty) {
    this.api = api;
    this.tasks = tasks;
    this.user = user;
    this.contactEmpty = contactEmpty;
    this.conversationEmpty = conversationEmpty;
    status.getStyleClass().add("feedback");
    status.setWrapText(true);

    var left = CourseUi.panel(contactHeading, contactRows);
    left.getStyleClass().add("message-contacts");
    left.setMinWidth(205);
    left.setPrefWidth(245);
    conversationTitle.getStyleClass().add("panel-title");
    history.getStyleClass().add("message-history");
    history.setFitToWidth(true);
    history.setHbarPolicy(ScrollBarPolicy.NEVER);
    history.setPrefHeight(300);
    history.setMinHeight(220);
    VBox.setVgrow(history, Priority.ALWAYS);
    body.setPromptText("Write a message…");
    body.setAccessibleText("Message text");
    body.setOnAction(e -> send());
    body.textProperty().addListener((property, oldValue, newValue) -> updateSend());
    HBox.setHgrow(body, Priority.ALWAYS);
    send.getStyleClass().add("primary-button");
    send.setOnAction(e -> send());
    var composer = new HBox(10, body, send);
    composer.getStyleClass().add("message-compose");
    var right = new VBox(16, conversationTitle, history, composer);
    right.getStyleClass().add("panel");
    right.setMinWidth(0);
    HBox.setHgrow(right, Priority.ALWAYS);
    var columns = new HBox(18, left, right);
    columns.getStyleClass().add("message-columns");
    var refresh = new Button("Refresh conversations");
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(e -> load(""));
    CourseUi.fill(this, CourseUi.page("Messages", subtitle, status, refresh, columns));
    load("");
  }

  private void load(String successMessage) {
    UiFeedback.loading(status, "Loading conversations…");
    tasks.run(() -> new Data(api.contacts(), api.history()), data -> {
      contacts = data.contacts();
      messages = data.messages();
      if (selectedId == null || contacts.stream().noneMatch(c -> c.id() == selectedId))
        selectedId = contacts.isEmpty() ? null : contacts.getFirst().id();
      showContacts();
      showConversation();
      UiFeedback.success(status, successMessage);
    }, error -> UiFeedback.error(status, error));
  }

  private void showContacts() {
    contactRows.getChildren().clear();
    if (contacts.isEmpty()) {
      contactRows.getChildren().add(UiFeedback.empty(contactEmpty));
    }
    for (var contact : contacts) {
      var name = new Label(contact.name());
      name.getStyleClass().add("message-contact-name");
      var detail = new Label(contact.detail() == null ? "Course faculty" : contact.detail());
      detail.getStyleClass().add("muted");
      var button = new Button();
      button.setGraphic(new VBox(3, name, detail));
      button.setAccessibleText("Conversation with " + contact.name());
      button.setMaxWidth(Double.MAX_VALUE);
      button.setUserData(contact.id());
      button.getStyleClass().add("message-contact");
      if (selectedId != null && contact.id() == selectedId) button.getStyleClass().add("selected");
      button.setOnAction(e -> { selectedId = contact.id(); selectContact(); showConversation(); });
      contactRows.getChildren().add(button);
    }
    updateSend();
  }

  private void selectContact() {
    for (var node : contactRows.getChildren()) {
      if (!(node instanceof Button button)) continue;
      button.getStyleClass().remove("selected");
      if (selectedId != null && selectedId.equals(button.getUserData()))
        button.getStyleClass().add("selected");
    }
  }

  private void showConversation() {
    var contact = contacts.stream().filter(c -> selectedId != null && c.id() == selectedId)
      .findFirst().orElse(null);
    conversationTitle.setText(contact == null ? "Select a conversation" : contact.name());
    messageRows.getChildren().clear();
    if (contact != null) {
      for (var item : messages) {
        if (item.senderId() != contact.id() && item.receiverId() != contact.id()) continue;
        var sender = new Label(item.sender());
        sender.getStyleClass().add("message-sender");
        var text = new Label(item.body());
        text.setWrapText(true);
        var time = new Label(CourseUi.date(item.sentAt()));
        time.getStyleClass().add("muted");
        var bubble = new VBox(5, sender, text, time);
        bubble.getStyleClass().add("message-bubble");
        if (item.senderId() == user.id()) bubble.getStyleClass().add("outgoing");
        var row = new HBox(bubble);
        row.setAlignment(item.senderId() == user.id() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageRows.getChildren().add(row);
      }
      if (messageRows.getChildren().isEmpty()) {
        messageRows.getChildren().add(UiFeedback.empty(conversationEmpty));
      }
    } else {
      messageRows.getChildren().add(UiFeedback.empty("Choose a conversation to view messages."));
    }
    Platform.runLater(() -> history.setVvalue(1));
    updateSend();
  }

  private void send() {
    if (busy || selectedId == null) return;
    String text = body.getText().trim();
    if (text.isEmpty()) { UiFeedback.error(status, "Write a message before sending."); return; }
    if (text.length() > 4000) { UiFeedback.error(status, "Messages must be at most 4000 characters."); return; }
    busy = true;
    updateSend();
    UiFeedback.loading(status, "Sending message…");
    long receiver = selectedId;
    tasks.run(() -> { api.send(receiver, text); return null; }, ignored -> {
      busy = false;
      body.clear();
      load("Message sent.");
    }, error -> { busy = false; updateSend(); UiFeedback.error(status, error); });
  }

  private void updateSend() {
    send.setDisable(busy || selectedId == null || body.getText().isBlank());
  }

  private record Data(List<ContactItem> contacts, List<MessageItem> messages) { }
}
