package com.thapar.ams.controller;

import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.MessageContracts.MessageItem;
import com.thapar.ams.contract.MessageContracts.SendMessageRequest;
import com.thapar.ams.service.FacultyMessageService;
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
@RequestMapping("/api/v1/faculty")
public class FacultyMessageController {
  private final FacultyMessageService messageService;

  public FacultyMessageController(FacultyMessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/messages")
  List<MessageItem> messages(Principal p) {
    return messageService.messages(p.getName());
  }

  @PostMapping("/messages")
  IdResponse send(Principal p, @Valid @RequestBody SendMessageRequest b) {
    return messageService.send(p.getName(), b.receiverId(), b.body());
  }
}
