package com.thapar.ams.controller;

import com.thapar.ams.contract.OverviewContracts.CalendarEvent;
import com.thapar.ams.contract.OverviewContracts.CreateEventRequest;
import com.thapar.ams.contract.OverviewContracts.FacultyDashboard;
import com.thapar.ams.service.FacultyOverviewService;
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
public class FacultyOverviewController {
  private final FacultyOverviewService overview;

  public FacultyOverviewController(FacultyOverviewService overview) {
    this.overview = overview;
  }

  @GetMapping("/dashboard")
  FacultyDashboard dashboard(Principal p) {
    return overview.dashboard(p.getName());
  }

  @GetMapping("/calendar")
  List<CalendarEvent> calendar(Principal p) {
    return overview.calendar(p.getName());
  }

  @PostMapping("/calendar")
  void event(Principal p, @Valid @RequestBody CreateEventRequest b) {
    overview.createEvent(p.getName(), b.title(), b.eventDate());
  }
}
