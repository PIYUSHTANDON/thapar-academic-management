package com.thapar.ams.controller;

import com.thapar.ams.contract.AccountContracts.ChangePasswordRequest;
import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AccountContracts.StudentProfile;
import com.thapar.ams.contract.AccountContracts.UpdateProfileRequest;
import com.thapar.ams.service.StudentAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentAccountController {
  private final StudentAccountService account;

  public StudentAccountController(StudentAccountService account) {
    this.account = account;
  }

  @GetMapping("/profile")
  StudentProfile profile(Principal p) {
    return account.profile(p.getName());
  }

  @GetMapping("/notifications")
  List<NotificationItem> notifications(Principal p) {
    return account.notifications(p.getName());
  }

  @PostMapping("/notifications/{id}/read")
  void read(Principal p, @PathVariable Long id) {
    account.markRead(p.getName(), id);
  }

  @PutMapping("/settings/profile")
  void updateProfile(Principal p, @Valid @RequestBody UpdateProfileRequest b) {
    account.updateProfile(p.getName(), b.name(), b.notificationsEnabled());
  }

  @PutMapping("/settings/password")
  void password(
    Principal p,
    @Valid @RequestBody ChangePasswordRequest b,
    HttpServletRequest request
  ) {
    account.changePassword(p.getName(), b.currentPassword(), b.newPassword());
    request.getSession().invalidate();
  }
}
