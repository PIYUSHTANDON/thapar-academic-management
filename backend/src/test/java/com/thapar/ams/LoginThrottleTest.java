package com.thapar.ams;

import static org.junit.jupiter.api.Assertions.*;

import com.thapar.ams.security.LoginThrottle;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LoginThrottleTest {

  @Test
  void eleventhAttemptIsThrottled() {
    var limiter = new LoginThrottle();
    for (int i = 0; i < 10; i++) limiter.check("test-source");
    var error = assertThrows(ResponseStatusException.class, () ->
      limiter.check("test-source")
    );
    assertEquals(429, error.getStatusCode().value());
  }

  @Test
  void sourceWindowsAreIndependent() {
    var limiter = new LoginThrottle();
    for (int i = 0; i < 10; i++) limiter.check("one-source");
    assertDoesNotThrow(() -> limiter.check("another-source"));
  }
}
