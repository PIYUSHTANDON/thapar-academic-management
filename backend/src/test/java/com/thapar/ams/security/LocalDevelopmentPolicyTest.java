package com.thapar.ams.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thapar.ams.entity.*;
import com.thapar.ams.repository.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LocalDevelopmentPolicyTest {

  LocalDevelopmentPolicy policy(String[] profiles, String url)
    throws Exception {
    var env = new MockEnvironment();
    env.setActiveProfiles(profiles);
    if (profiles.length == 0) env.setDefaultProfiles("local");
    var ds = mock(DataSource.class);
    var c = mock(Connection.class);
    var meta = mock(DatabaseMetaData.class);
    when(ds.getConnection()).thenReturn(c);
    when(c.getMetaData()).thenReturn(meta);
    when(meta.getURL()).thenReturn(url);
    return new LocalDevelopmentPolicy(env, ds);
  }

  @Test
  void permitsOnlyLocalPostgres() throws Exception {
    assertTrue(
      policy(
        new String[] { "local" },
        "jdbc:postgresql://127.0.0.1:55432/thapar_ams?currentSchema=academic"
      ).permitsDemoAccounts()
    );
    assertTrue(
      policy(
        new String[] {},
        "jdbc:postgresql://localhost:55432/thapar_ams"
      ).permitsDemoAccounts()
    );
    assertFalse(
      policy(
        new String[] { "local" },
        "jdbc:postgresql://db.example.com:5432/thapar_ams"
      ).permitsDemoAccounts()
    );
    assertFalse(
      policy(
        new String[] { "local" },
        "jdbc:h2:mem:demo"
      ).permitsDemoAccounts()
    );
  }

  @Test
  void rejectsStagingProductionAndMixedProfiles() throws Exception {
    for (var profiles : List.of(
      new String[] { "staging" },
      new String[] { "prod" },
      new String[] { "production" },
      new String[] { "local", "staging" },
      new String[] { "local", "production" }
    ))
      assertFalse(
        policy(
          profiles,
          "jdbc:postgresql://127.0.0.1:55432/thapar_ams"
        ).permitsDemoAccounts(),
        Arrays.toString(profiles)
      );
  }

  @Test
  void copiedLocalAccountCannotAuthenticateOutsideLocalProfile() {
    var repo = mock(UserRepository.class);
    var local = mock(LocalDevelopmentPolicy.class);
    var user = new User();
    user.identifier = "FACULTY001";
    user.name = "Demo Faculty";
    user.role = Role.FACULTY;
    user.localOnly = true;
    user.passwordHash = "unused";
    when(repo.findByIdentifier(user.identifier)).thenReturn(Optional.of(user));
    when(local.permitsDemoAccounts()).thenReturn(false);
    var config = new SecurityConfig();
    assertThrows(
      org.springframework.security.core.userdetails.UsernameNotFoundException.class,
      () -> config.users(repo, local).loadUserByUsername("FACULTY001")
    );
  }
}
