package com.thapar.ams.dto;

import com.thapar.ams.entity.Role;
import jakarta.validation.constraints.*;

public record LoginRequest(
  @NotBlank @Size(max = 120) String identifier,
  @NotBlank @Size(max = 128) String password,
  @NotNull Role role,
  boolean rememberMe
) {
  @Override
  public String toString() {
    return "LoginRequest[credentials=REDACTED, role=" + role + "]";
  }
}
