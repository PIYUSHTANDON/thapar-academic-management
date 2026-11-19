package com.thapar.ams.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthContracts {
  private AuthContracts() { }

  public enum UserRole {
    STUDENT,
    FACULTY,
    ADMIN
  }

  public record CsrfTokenResponse(String token, String headerName) { }

  public record LoginRequest(
    @NotBlank @Size(max = 120) String identifier,
    @NotBlank @Size(max = 128) String password,
    @NotNull UserRole role,
    boolean rememberMe
  ) {
    @Override
    public String toString() {
      return "LoginRequest[credentials=REDACTED, role=" + role + "]";
    }
  }

  public record UserResponse(
    long id,
    String name,
    String identifier,
    UserRole role,
    boolean notificationsEnabled
  ) { }

  public record SessionResponse(
    boolean authenticated,
    @JsonInclude(JsonInclude.Include.NON_NULL) UserResponse user
  ) { }

  public record ApiErrorResponse(
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> errors
  ) {
    public ApiErrorResponse(String message) {
      this(message, null);
    }
  }
}
