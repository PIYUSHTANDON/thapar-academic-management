package com.thapar.ams.service;

import com.thapar.ams.contract.AccountContracts.NotificationItem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyAccountService {
  private final FacultyService s;
  private final PasswordEncoder encoder;

  public FacultyAccountService(FacultyService s, PasswordEncoder encoder) {
    this.s = s;
    this.encoder = encoder;
  }

  public List<NotificationItem> notifications(String name) {
    return s.notifications().findByUserId(s.user(name).id).stream()
      .map(n -> new NotificationItem(n.id, n.title, n.read))
      .toList();
  }

  public void markRead(String name, Long id) {
    var notification = s.notifications().findById(id).orElseThrow();
    if (!notification.user.id.equals(s.user(name).id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    notification.read = true;
    s.notifications().save(notification);
  }

  public void updateProfile(String name, String displayName, boolean notificationsEnabled) {
    var user = s.user(name);
    user.name = displayName;
    user.notificationsEnabled = notificationsEnabled;
    s.users().save(user);
  }

  public void changePassword(String name, String current, String next) {
    var user = s.user(name);
    if (!encoder.matches(current, user.passwordHash))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    user.passwordHash = encoder.encode(next);
    s.users().save(user);
  }
}
