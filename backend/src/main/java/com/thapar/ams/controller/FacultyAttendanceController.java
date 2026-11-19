package com.thapar.ams.controller;

import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import com.thapar.ams.contract.AttendanceContracts.FacultyAttendanceRecord;
import com.thapar.ams.contract.AttendanceContracts.OpenSessionRequest;
import com.thapar.ams.service.FacultyAttendanceService;
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
public class FacultyAttendanceController {
  private final FacultyAttendanceService attendanceService;

  public FacultyAttendanceController(FacultyAttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping("/attendance")
  List<FacultyAttendanceRecord> attendance(Principal p) {
    return attendanceService.records(p.getName());
  }

  @PostMapping("/attendance/sessions")
  AttendanceToken token(Principal p, @Valid @RequestBody OpenSessionRequest b) {
    return attendanceService.openSession(p.getName(), b.sessionId());
  }
}
