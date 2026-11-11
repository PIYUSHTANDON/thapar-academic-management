package com.thapar.ams.controller;

import com.thapar.ams.dto.*;
import com.thapar.ams.repository.*;
import com.thapar.ams.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationManager manager;
  private final UserRepository users;
  private final LoginThrottle throttle;
  private final HttpSessionSecurityContextRepository contexts =
    new HttpSessionSecurityContextRepository();

  public AuthController(
    AuthenticationManager m,
    UserRepository u,
    LoginThrottle t
  ) {
    manager = m;
    users = u;
    throttle = t;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of(
      "token",
      token.getToken(),
      "headerName",
      token.getHeaderName()
    );
  }

  @PostMapping("/login")
  public Map<String, Object> login(
    @Valid @RequestBody LoginRequest body,
    HttpServletRequest req,
    HttpServletResponse res
  ) {
    throttle.check(req.getRemoteAddr());
    Authentication auth;
    try {
      auth = manager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(
          body.identifier(),
          body.password()
        )
      );
    } catch (AuthenticationException e) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "Invalid credentials or role"
      );
    }
    var u = users.findByIdentifier(auth.getName()).orElseThrow();
    if (u.role != body.role()) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Invalid credentials or role"
    );
    req.getSession();
    req.changeSessionId();
    req.getSession().setMaxInactiveInterval(body.rememberMe() ? 28800 : 1800);
    new HttpSessionCsrfTokenRepository().saveToken(null, req, res);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, req, res);
    return me(auth);
  }

  @GetMapping("/session")
  public Map<String, Object> session(Authentication auth) {
    if (
      auth == null ||
      auth instanceof
        org.springframework.security.authentication.AnonymousAuthenticationToken
    ) return Map.of("authenticated", false);
    return Map.of("authenticated", true, "user", me(auth));
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication auth) {
    var u = users.findByIdentifier(auth.getName()).orElseThrow();
    return Map.of(
      "id",
      u.id,
      "name",
      u.name,
      "identifier",
      u.identifier,
      "role",
      u.role,
      "notificationsEnabled",
      u.notificationsEnabled
    );
  }
}
