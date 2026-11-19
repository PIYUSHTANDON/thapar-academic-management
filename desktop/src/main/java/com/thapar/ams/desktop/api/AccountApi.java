package com.thapar.ams.desktop.api;

import com.thapar.ams.contract.AccountContracts.ChangePasswordRequest;
import com.thapar.ams.contract.AccountContracts.StudentProfile;
import com.thapar.ams.contract.AccountContracts.UpdateProfileRequest;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;

public final class AccountApi {
  private final ApiClient api;
  private final String role;

  public AccountApi(ApiClient api, UserRole role) {
    if (role == UserRole.ADMIN) throw new IllegalArgumentException("Settings require a faculty or student role.");
    this.api = api;
    this.role = role == UserRole.FACULTY ? "faculty" : "student";
  }

  public Profile load() {
    UserResponse user = api.get("auth/me", UserResponse.class);
    StudentProfile student = role.equals("student")
      ? api.get("student/profile", StudentProfile.class) : null;
    return new Profile(user, student);
  }

  public UserResponse updateProfile(String name, boolean notificationsEnabled) {
    api.send("PUT", role + "/settings/profile", new UpdateProfileRequest(name, notificationsEnabled));
    return api.get("auth/me", UserResponse.class);
  }

  public void changePassword(String currentPassword, String newPassword) {
    api.send("PUT", role + "/settings/password", new ChangePasswordRequest(currentPassword, newPassword));
    api.resetCsrf();
  }

  public record Profile(UserResponse user, StudentProfile student) { }
}
