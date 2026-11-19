package com.thapar.ams.controller;

import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.StudentDashboard;
import com.thapar.ams.service.StudentOverviewService;
import java.security.Principal;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentOverviewController {
  private final StudentOverviewService overview;

  public StudentOverviewController(StudentOverviewService overview) {
    this.overview = overview;
  }

  @GetMapping("/dashboard")
  StudentDashboard dashboard(Principal p) {
    return overview.dashboard(p.getName());
  }

  @GetMapping("/calendar")
  List<CalendarEvent> calendar(Principal p) {
    return overview.calendar(p.getName());
  }
}
