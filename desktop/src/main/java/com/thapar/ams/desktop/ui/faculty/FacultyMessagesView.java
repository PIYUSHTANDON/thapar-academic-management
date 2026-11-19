package com.thapar.ams.desktop.ui.faculty;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.desktop.api.MessageApi;
import com.thapar.ams.desktop.ui.MessagesView;
import com.thapar.ams.desktop.ui.TaskRunner;

public final class FacultyMessagesView extends MessagesView {
  public FacultyMessagesView(MessageApi api, TaskRunner tasks, UserResponse user) {
    super(api, tasks, user, "Stay connected with students in your assigned courses.",
      "Conversations", "No enrolled students to message.", "Start a conversation with your student.");
  }
}
