/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.PlayerState;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.Test;

class DefaultPlayerTest {
  @Test
  void testNewInstance() {
    Player actualNewInstanceResult = DefaultPlayer.newInstance("Name");
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertFalse(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertFalse(actualNewInstanceResult.getSession().isPresent());
    assertEquals("Name", actualNewInstanceResult.getIdentity());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance2() {
    Player actualNewInstanceResult = DefaultPlayer.newInstance("Name", mock(Session.class));
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertFalse(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertEquals("Name", actualNewInstanceResult.getIdentity());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance3() {
    Player actualNewInstanceResult = DefaultPlayer.newInstance("Name", null);
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertFalse(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertFalse(actualNewInstanceResult.getSession().isPresent());
    assertEquals("Name", actualNewInstanceResult.getIdentity());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testStateTransitions() {
    Player player = DefaultPlayer.newInstance("Test");
    player.setState(null);
    assertTrue(player.isState(null));
    PlayerState stateA = new PlayerState() {};
    PlayerState stateB = new PlayerState() {};
    assertTrue(player.transitionState(null, stateA));
    assertTrue(player.isState(stateA));
    assertFalse(player.transitionState(stateB, stateA));
  }

  @Test
  void testActivationAndLogin() {
    Player player = DefaultPlayer.newInstance("Test");
    player.setActivated(true);
    assertTrue(player.isActivated());
    player.setLoggedIn(true);
    assertTrue(player.isLoggedIn());
    assertTrue(player.getLastLoggedInTime() > 0);
    player.setLoggedIn(false);
    assertFalse(player.isLoggedIn());
  }

  @Test
  void testSessionAssignment() {
    Player player = DefaultPlayer.newInstance("Test");
    Session session = mock(Session.class);
    player.setSession(session);
    assertTrue(player.getSession().isPresent());
    player.setSession(null);
    assertFalse(player.getSession().isPresent());
  }

  @Test
  void testRoomAssignment() {
    Player player = DefaultPlayer.newInstance("Test");
    assertFalse(player.isInRoom());
    Room room = mock(Room.class);
    player.setCurrentRoom(room);
    assertTrue(player.isInRoom());
    assertTrue(player.getCurrentRoom().isPresent());
    player.setCurrentRoom(null);
    assertFalse(player.isInRoom());
  }

  @Test
  void testRoleTransitions() {
    Player player = DefaultPlayer.newInstance("Test");
    assertTrue(player.transitionRole(PlayerRoleInRoom.SPECTATOR, PlayerRoleInRoom.PARTICIPANT));
    assertEquals(PlayerRoleInRoom.PARTICIPANT, player.getRoleInRoom());
    assertFalse(player.transitionRole(PlayerRoleInRoom.SPECTATOR, PlayerRoleInRoom.PARTICIPANT));
  }

  @Test
  void testPlayerSlot() {
    Player player = DefaultPlayer.newInstance("Test");
    player.setPlayerSlotInCurrentRoom(5);
    assertEquals(5, player.getPlayerSlotInCurrentRoom());
  }

  @Test
  void testPropertyMap() {
    Player player = DefaultPlayer.newInstance("Test");
    player.setProperty("key", "value");
    assertEquals("value", player.getProperty("key"));
    assertTrue(player.containsProperty("key"));
    player.removeProperty("key");
    assertFalse(player.containsProperty("key"));
    player.setProperty("a", 1);
    player.clearProperties();
    assertFalse(player.containsProperty("a"));
  }

  @Test
  void testIdleAndDeportation() {
    Player player = DefaultPlayer.newInstance("Test");
    player.configureMaxIdleTimeInSeconds(0);
    assertFalse(player.isIdle());
    player.setNeverDeported(true);
    assertTrue(player.isNeverDeported());
    player.configureMaxIdleTimeNeverDeportedInSeconds(0);
    assertFalse(player.isIdleNeverDeported());
  }

  @Test
  void testEqualsAndHashCode() {
    Player player1 = DefaultPlayer.newInstance("Test");
    Player player2 = DefaultPlayer.newInstance("Test");
    Player player3 = DefaultPlayer.newInstance("Other");
    assertEquals(player1, player2);
    assertNotEquals(player1, player3);
    assertEquals(player1.hashCode(), player2.hashCode());
  }

  @Test
  void testToString() {
    Player player = DefaultPlayer.newInstance("Test");
    assertTrue(player.toString().contains("identity='Test'"));
  }

  @Test
  void testClean() {
    Player player = DefaultPlayer.newInstance("Test");
    player.setActivated(true);
    player.setProperty("a", 1);
    player.setCurrentRoom(mock(Room.class));
    player.setSession(mock(Session.class));
    player.clean();
    assertFalse(player.isActivated());
    assertFalse(player.isInRoom());
    assertFalse(player.getSession().isPresent());
    assertFalse(player.containsProperty("a"));
  }
}
