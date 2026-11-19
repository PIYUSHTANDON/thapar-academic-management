package com.thapar.ams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AmsApplication {

  static {
    // PostgreSQL requires the canonical name rather than Windows' legacy Asia/Calcutta alias.
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
  }

  public static void main(String[] args) {
    SpringApplication.run(AmsApplication.class, args);
  }
}
