package com.thapar.ams.controller;

import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.MessageContracts.ContactItem;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.contract.MessageContracts.SendMessageRequest;
import com.thapar.ams.service.StudentMessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentMessageController {
  private final StudentMessageService messageService;

  public StudentMessageController(StudentMessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/messages/contacts")
  List<ContactItem> contacts(Principal p) {
    return messageService.teachers(p.getName());
  }

  @GetMapping("/messages")
  List<MessageItem> messages(Principal p) {
    return messageService.messages(p.getName());
  }

  @PostMapping("/messages")
  IdResponse message(Principal p, @Valid @RequestBody SendMessageRequest b) {
    return messageService.send(p.getName(), b.receiverId(), b.body());
  }
}
