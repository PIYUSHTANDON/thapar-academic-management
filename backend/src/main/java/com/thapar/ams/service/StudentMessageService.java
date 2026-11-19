package com.thapar.ams.service;

import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.MessageContracts.ContactItem;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.entity.Message;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentMessageService {
  private final FacultyService s;
  private final StudentService access;

  public StudentMessageService(FacultyService s, StudentService access) {
    this.s = s;
    this.access = access;
  }

  public List<ContactItem> teachers(String name) {
    return access.enrolled(name)
      .stream()
      .map(c -> c.faculty.user)
      .distinct()
      .map(u -> new ContactItem(u.id, u.name))
      .toList();
  }

  public boolean canMessage(String name, Long id) {
    return teachers(name).stream().anyMatch(t -> Long.valueOf(t.id()).equals(id));
  }

  public List<MessageItem> messages(String name) {
    var u = access.student(name).user;
    return s.messages()
      .findBySenderIdOrReceiverIdOrderBySentAtAsc(u.id, u.id)
      .stream()
      .filter(m -> canMessage(name, m.sender.id.equals(u.id) ? m.receiver.id : m.sender.id))
      .map(m -> new MessageItem(
        m.id,
        m.sender.id,
        m.sender.name,
        m.receiver.id,
        m.receiver.name,
        m.body,
        m.sentAt
      ))
      .toList();
  }

  public IdResponse send(String name, Long receiverId, String body) {
    if (!canMessage(name, receiverId))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can message only teachers of enrolled courses");
    var message = new Message();
    message.sender = access.student(name).user;
    message.receiver = s.users().findById(receiverId).orElseThrow();
    message.body = body;
    message.sentAt = StudentService.now();
    s.messages().save(message);
    return new IdResponse(message.id);
  }
}
