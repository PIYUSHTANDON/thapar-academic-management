package com.thapar.ams.service;

import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.entity.Message;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyMessageService {
  private final FacultyService s;
  private final FacultyCourseService course;

  public FacultyMessageService(FacultyService s, FacultyCourseService course) {
    this.s = s;
    this.course = course;
  }

  public boolean canMessage(String name, Long receiver) {
    return course.roster(name)
      .stream()
      .anyMatch(entry -> Long.valueOf(entry.userId()).equals(receiver));
  }

  public List<MessageItem> messages(String name) {
    var u = s.user(name);
    return s.messages()
      .findBySenderIdOrReceiverIdOrderBySentAtAsc(u.id, u.id)
      .stream()
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
    if (!canMessage(name, receiverId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    var message = new Message();
    message.sender = s.user(name);
    message.receiver = s.users().findById(receiverId).orElseThrow();
    message.body = body;
    message.sentAt = LocalDateTime.now();
    s.messages().save(message);
    return new IdResponse(message.id);
  }
}
