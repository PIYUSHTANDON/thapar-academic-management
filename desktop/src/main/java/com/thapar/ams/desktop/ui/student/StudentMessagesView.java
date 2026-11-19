package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.desktop.api.MessageApi;
import com.thapar.ams.desktop.ui.MessagesView;
import com.thapar.ams.desktop.ui.TaskRunner;

public final class StudentMessagesView extends MessagesView {
  public StudentMessagesView(MessageApi api, TaskRunner tasks, UserResponse user) {
    super(api, tasks, user, "Connect with the teachers of your enrolled courses.",
      "Your teachers", "No course faculty available.", "Start a conversation with your teacher.");
  }
}
