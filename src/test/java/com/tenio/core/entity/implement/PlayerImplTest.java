package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tenio.core.entity.Player;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.Test;

class PlayerImplTest {
  @Test
  void testNewInstance() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name");
    assertNull(actualNewInstanceResult.getCurrentRoom());
    assertEquals(
        "{ name: Name, session: false, loggedIn: false, spectator: true, activated: false }",
        actualNewInstanceResult.toString());
    assertTrue(actualNewInstanceResult.isSpectator());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertNull(actualNewInstanceResult.getSession());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance2() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name", mock(Session.class));
    assertNull(actualNewInstanceResult.getCurrentRoom());
    assertEquals(
        "{ name: Name, session: true, loggedIn: false, spectator: true, activated: false }",
        actualNewInstanceResult.toString());
    assertTrue(actualNewInstanceResult.isSpectator());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance3() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name", null);
    assertNull(actualNewInstanceResult.getCurrentRoom());
    assertEquals(
        "{ name: Name, session: false, loggedIn: false, spectator: true, activated: false }",
        actualNewInstanceResult.toString());
    assertTrue(actualNewInstanceResult.isSpectator());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertNull(actualNewInstanceResult.getSession());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }
}

