package com.thapar.ams.security;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LocalDevelopmentPolicy {

  private final Environment env;
  private final DataSource database;

  public LocalDevelopmentPolicy(Environment env, DataSource database) {
    this.env = env;
    this.database = database;
  }

  public boolean permitsDemoAccounts() {
    String[] profiles =
      env.getActiveProfiles().length == 0
        ? env.getDefaultProfiles()
        : env.getActiveProfiles();
    if (profiles.length != 1 || !profiles[0].equals("local")) return false;
    try (var connection = database.getConnection()) {
      return connection.getMetaData().getURL().startsWith("jdbc:h2:");
    } catch (java.sql.SQLException e) {
      return false;
    }
  }
}
