package com.thapar.ams.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class MessageContracts {
  private MessageContracts() { }

  public record ContactItem(
    long id,
    String name,
    @JsonInclude(JsonInclude.Include.NON_NULL) String detail
  ) {
    public ContactItem(long id, String name) {
      this(id, name, null);
    }
  }

  public record MessageItem(
    long id,
    long senderId,
    String sender,
    long receiverId,
    String receiver,
    String body,
    LocalDateTime sentAt
  ) { }

  public record SendMessageRequest(
    @NotNull Long receiverId,
    @NotBlank @Size(max = 4000) String body
  ) { }
}
