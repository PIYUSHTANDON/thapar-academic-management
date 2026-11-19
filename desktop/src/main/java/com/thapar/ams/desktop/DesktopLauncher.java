package com.thapar.ams.desktop;

public final class DesktopLauncher {
  private DesktopLauncher() {}

  public static void main(String[] args) throws Exception {
    try (var managed = ManagedBackend.startIfRequested()) {
      App.main(args);
    }
  }
}
