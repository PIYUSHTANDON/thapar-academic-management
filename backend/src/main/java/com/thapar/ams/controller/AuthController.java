package com.thapar.ams.controller;

import com.thapar.ams.contract.AuthContracts.CsrfTokenResponse;
import com.thapar.ams.contract.AuthContracts.LoginRequest;
import com.thapar.ams.contract.AuthContracts.SessionResponse;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.repository.UserRepository;
import com.thapar.ams.security.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
  public CsrfTokenResponse csrf(CsrfToken token) {
    return new CsrfTokenResponse(token.getToken(), token.getHeaderName());
  }

  @PostMapping("/login")
  public UserResponse login(
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
    if (!u.role.name().equals(body.role().name())) throw new ResponseStatusException(
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
  public SessionResponse session(Authentication auth) {
    if (auth == null || auth instanceof AnonymousAuthenticationToken)
      return new SessionResponse(false, null);
    return new SessionResponse(true, me(auth));
  }

  @GetMapping("/me")
  public UserResponse me(Authentication auth) {
    var u = users.findByIdentifier(auth.getName()).orElseThrow();
    return new UserResponse(
      u.id,
      u.name,
      u.identifier,
      UserRole.valueOf(u.role.name()),
      u.notificationsEnabled
    );
  }
}
