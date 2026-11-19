package com.thapar.ams.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class ManagedBackendTest {
  @Test void fallsBackToFreePortWhenPreferredPortIsOccupied() throws Exception {
    try (var occupied = new ServerSocket(0)) {
      int chosen = ManagedBackend.selectPort(occupied.getLocalPort());
      assertNotEquals(occupied.getLocalPort(), chosen);
      assertTrue(chosen > 0);
    }
  }

  @Test void selectsEphemeralPortWhenAvailable() throws Exception {
    int chosen = ManagedBackend.selectPort(0);
    assertTrue(chosen > 0);
  }
}
