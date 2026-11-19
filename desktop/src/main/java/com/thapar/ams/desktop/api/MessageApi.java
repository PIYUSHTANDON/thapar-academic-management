package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.contract.CourseContracts.RosterEntry;
import com.thapar.ams.contract.MessageContracts.ContactItem;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.contract.MessageContracts.SendMessageRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class MessageApi {
  private final ApiClient api;
  private final String role;

  public MessageApi(ApiClient api, UserRole role) {
    if (role == UserRole.ADMIN) throw new IllegalArgumentException("Messages require a faculty or student role.");
    this.api = api;
    this.role = role == UserRole.FACULTY ? "faculty" : "student";
  }

  public List<ContactItem> contacts() {
    if (role.equals("student"))
      return api.get("student/messages/contacts", new TypeReference<>() { });
    List<RosterEntry> roster = api.get("faculty/students", new TypeReference<>() { });
    var unique = new LinkedHashMap<Long, ContactItem>();
    for (var student : roster)
      unique.putIfAbsent(student.userId(), new ContactItem(student.userId(), student.name(),
        student.enrollmentNumber()));
    return new ArrayList<>(unique.values());
  }

  public List<MessageItem> history() {
    return api.get(role + "/messages", new TypeReference<>() { });
  }

  public IdResponse send(long receiverId, String body) {
    return api.post(role + "/messages", new SendMessageRequest(receiverId, body), IdResponse.class);
  }
}
