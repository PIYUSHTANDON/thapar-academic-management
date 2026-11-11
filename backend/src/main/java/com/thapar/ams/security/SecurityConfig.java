package com.thapar.ams.security;

import com.thapar.ams.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(
    UserRepository repo,
    LocalDevelopmentPolicy localPolicy
  ) {
    return id -> {
      var u = repo
        .findByIdentifier(id)
        .orElseThrow(() ->
          new UsernameNotFoundException("Invalid credentials")
        );
      if (
        u.localOnly && !localPolicy.permitsDemoAccounts()
      ) throw new UsernameNotFoundException("Invalid credentials");
      return User.withUsername(u.identifier)
        .password(u.passwordHash)
        .roles(u.role.name())
        .build();
    };
  }

  @Bean
  AuthenticationManager manager(
    UserDetailsService users,
    PasswordEncoder encoder
  ) {
    var p = new DaoAuthenticationProvider(users);
    p.setPasswordEncoder(encoder);
    return new ProviderManager(p);
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/v1/health",
            "/api/v1/auth/csrf",
            "/api/v1/auth/session",
            "/api/v1/auth/login",
            "/error"
          )
          .permitAll()
          .requestMatchers("/api/v1/faculty/**")
          .hasRole("FACULTY")
          .requestMatchers("/api/v1/student/**")
          .hasRole("STUDENT")
          .anyRequest()
          .authenticated()
      )
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((r, s, x) -> s.sendError(401))
          .accessDeniedHandler((r, s, x) -> s.sendError(403))
      )
      .logout(l ->
        l
          .logoutUrl("/api/v1/auth/logout")
          .invalidateHttpSession(true)
          .deleteCookies("JSESSIONID")
          .logoutSuccessHandler((r, s, a) -> s.setStatus(204))
      )
      .build();
  }
}
