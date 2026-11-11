package com.thapar.ams.security;

import java.time.*;
import java.util.concurrent.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LoginThrottle {

  record Window(Instant start, int attempts) {}

  private final ConcurrentHashMap<String, Window> attempts =
    new ConcurrentHashMap<>();

  public void check(String key) {
    var now = Instant.now();
    attempts
      .entrySet()
      .removeIf(e -> e.getValue().start.plusSeconds(900).isBefore(now));
    if (attempts.size() > 10000) throw new ResponseStatusException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Try again later"
    );
    var w = attempts.compute(key, (k, v) ->
      v == null ? new Window(now, 1) : new Window(v.start, v.attempts + 1)
    );
    if (w.attempts > 10) throw new ResponseStatusException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Too many attempts. Try again in 15 minutes."
    );
  }
}
