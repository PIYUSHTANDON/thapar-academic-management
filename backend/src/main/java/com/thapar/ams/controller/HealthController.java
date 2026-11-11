package com.thapar.ams.controller;

import java.util.Map;
import org.springframework.boot.availability.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {

  private final ApplicationAvailability availability;

  public HealthController(ApplicationAvailability availability) {
    this.availability = availability;
  }

  @GetMapping("/api/v1/health")
  public ResponseEntity<?> ready() {
    boolean ready =
      availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;
    return ResponseEntity.status(ready ? 200 : 503).body(
      Map.of("status", ready ? "ready" : "starting")
    );
  }
}
