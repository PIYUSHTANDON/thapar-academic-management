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
      return isLocalPostgresUrl(connection.getMetaData().getURL());
    } catch (java.sql.SQLException e) {
      return false;
    }
  }

  static boolean isLocalPostgresUrl(String url) {
    if (url == null) return false;
    return (
      url.startsWith("jdbc:postgresql://127.0.0.1:") ||
      url.startsWith("jdbc:postgresql://127.0.0.1/") ||
      url.startsWith("jdbc:postgresql://localhost:") ||
      url.startsWith("jdbc:postgresql://localhost/") ||
      url.startsWith("jdbc:postgresql://[::1]:") ||
      url.startsWith("jdbc:postgresql://[::1]/")
    );
  }
}
