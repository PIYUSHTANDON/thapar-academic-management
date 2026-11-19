package com.thapar.ams.service;

import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import com.thapar.ams.contract.AttendanceContracts.FacultyAttendanceRecord;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyAttendanceService {
  private final FacultyService s;

  public FacultyAttendanceService(FacultyService s) {
    this.s = s;
  }

  public List<FacultyAttendanceRecord> records(String name) {
    return s.attendance()
      .findByClassSessionCourseFacultyId(s.faculty(name).id)
      .stream()
      .map(a -> new FacultyAttendanceRecord(
        a.id,
        a.student.user.name,
        a.classSession.id,
        a.classSession.course.title,
        a.recordedAt
      ))
      .toList();
  }

  public AttendanceToken openSession(String name, Long sessionId) {
    var session = s.sessions().findById(sessionId).orElseThrow();
    s.course(name, session.course.id);
    var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    if (session.cancelled || now.isBefore(session.startsAt.minusMinutes(15)) || now.isAfter(session.endsAt))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance opens 15 minutes before class and closes at its end");
    var bytes = new byte[32];
    new java.security.SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    session.tokenHash = FacultyService.hash(token);
    session.tokenExpiresAt = now.plusMinutes(3);
    s.sessions().save(session);
    return new AttendanceToken(token, session.tokenExpiresAt, session.id);
  }
}
