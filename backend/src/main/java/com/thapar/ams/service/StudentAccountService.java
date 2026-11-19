package com.thapar.ams.service;

import com.thapar.ams.contract.AccountContracts.NotificationItem;
import com.thapar.ams.contract.AccountContracts.StudentProfile;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentAccountService {
  private final StudentService access;
  private final FacultyService s;
  private final PasswordEncoder encoder;

  public StudentAccountService(StudentService access, FacultyService s, PasswordEncoder encoder) {
    this.access = access;
    this.s = s;
    this.encoder = encoder;
  }

  public StudentProfile profile(String name) {
    var st = access.student(name);
    return new StudentProfile(
      st.id,
      st.user.name,
      st.user.identifier,
      st.enrollmentNumber,
      st.program,
      st.academicYear,
      st.semester,
      st.officialCgpa,
      st.user.notificationsEnabled,
      st.user.localOnly || Objects.toString(st.program, "").toLowerCase().contains("synthetic")
    );
  }

  public List<NotificationItem> notifications(String name) {
    return s.notifications().findByUserId(access.student(name).user.id).stream()
      .map(n -> new NotificationItem(n.id, n.title, n.read))
      .toList();
  }

  public void markRead(String name, Long id) {
    var notification = s.notifications().findById(id).orElseThrow();
    if (!notification.user.id.equals(access.student(name).user.id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    notification.read = true;
    s.notifications().save(notification);
  }

  public void updateProfile(String name, String displayName, boolean notificationsEnabled) {
    var user = access.student(name).user;
    user.name = displayName;
    user.notificationsEnabled = notificationsEnabled;
    s.users().save(user);
  }

  public void changePassword(String name, String current, String next) {
    var user = access.student(name).user;
    if (!encoder.matches(current, user.passwordHash))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    user.passwordHash = encoder.encode(next);
    s.users().save(user);
  }
}
