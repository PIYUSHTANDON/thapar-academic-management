package com.thapar.ams.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class AccountContracts {
  private AccountContracts() { }

  public record StudentProfile(
    long id,
    String name,
    String identifier,
    String enrollmentNumber,
    String program,
    Integer academicYear,
    Integer semester,
    BigDecimal officialCgpa,
    boolean notificationsEnabled,
    boolean synthetic
  ) { }

  public record NotificationItem(long id, String title, boolean read) { }

  public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String name,
    boolean notificationsEnabled
  ) { }

  public record ChangePasswordRequest(
    @NotBlank @Size(max = 128) String currentPassword,
    @NotBlank @Size(min = 12, max = 128) String newPassword
  ) {
    @Override
    public String toString() {
      return "ChangePasswordRequest[REDACTED]";
    }
  }
}
