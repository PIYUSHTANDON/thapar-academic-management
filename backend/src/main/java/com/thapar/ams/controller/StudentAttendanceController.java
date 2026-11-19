package com.thapar.ams.controller;

import com.thapar.ams.contract.AttendanceContracts.ClaimAttendanceRequest;
import com.thapar.ams.contract.AttendanceContracts.MessageResponse;
import com.thapar.ams.contract.AttendanceContracts.StudentAttendanceSummary;
import com.thapar.ams.service.StudentAttendanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentAttendanceController {
  private final StudentAttendanceService attendanceService;

  public StudentAttendanceController(StudentAttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping("/attendance")
  StudentAttendanceSummary attendance(Principal p) {
    return attendanceService.attendance(p.getName());
  }

  @PostMapping("/attendance/claim")
  MessageResponse claim(Principal p, @Valid @RequestBody ClaimAttendanceRequest b) {
    return attendanceService.claim(p.getName(), b.token());
  }
}
