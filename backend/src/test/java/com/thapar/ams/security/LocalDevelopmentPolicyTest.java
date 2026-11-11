package com.thapar.ams.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thapar.ams.entity.*;
import com.thapar.ams.repository.*;
import com.thapar.ams.security.*;
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
  void permitsOnlyLocalH2() throws Exception {
    assertTrue(
      policy(new String[] { "local" }, "jdbc:h2:mem:demo").permitsDemoAccounts()
    );
    assertTrue(
      policy(new String[] {}, "jdbc:h2:mem:demo").permitsDemoAccounts()
    );
    assertFalse(
      policy(
        new String[] { "local" },
        "jdbc:mysql://localhost/ams"
      ).permitsDemoAccounts()
    );
  }

  @Test
  void rejectsMysqlStagingProductionAndMixedProfiles() throws Exception {
    for (var profiles : List.of(
      new String[] { "mysql" },
      new String[] { "staging" },
      new String[] { "prod" },
      new String[] { "production" },
      new String[] { "local", "mysql" },
      new String[] { "local", "staging" },
      new String[] { "local", "production" }
    ))
      assertFalse(
        policy(profiles, "jdbc:h2:mem:demo").permitsDemoAccounts(),
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
