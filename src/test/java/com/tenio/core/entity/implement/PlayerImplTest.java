package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.Test;

class PlayerImplTest {
  @Test
  void testNewInstance() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name");
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals("Player{name='Name', properties={}, session=null, currentRoom=null, state=null, roleInRoom=SPECTATOR, lastLoginTime=0, lastJoinedRoomTime=1666206549193, playerSlotInCurrentRoom=-1, loggedIn=false, activated=false, hasSession=false}",
        actualNewInstanceResult.toString());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertFalse(actualNewInstanceResult.getSession().isPresent());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance2() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name", mock(Session.class));
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals(
        "Player{name='Name', properties={}, session=Mock for Session, hashCode: 1192603187, currentRoom=null, state=null, roleInRoom=SPECTATOR, lastLoginTime=0, lastJoinedRoomTime=1666206366622, playerSlotInCurrentRoom=-1, loggedIn=false, activated=false, hasSession=true}",
        actualNewInstanceResult.toString());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }

  @Test
  void testNewInstance3() {
    Player actualNewInstanceResult = PlayerImpl.newInstance("Name", null);
    assertTrue(actualNewInstanceResult.getCurrentRoom().isEmpty());
    assertEquals(
        "Player{name='Name', properties={}, session=null, currentRoom=null, state=null, roleInRoom=SPECTATOR, lastLoginTime=0, lastJoinedRoomTime=1666206366627, playerSlotInCurrentRoom=-1, loggedIn=false, activated=false, hasSession=false}",
        actualNewInstanceResult.toString());
    assertEquals(PlayerRoleInRoom.SPECTATOR, actualNewInstanceResult.getRoleInRoom());
    assertFalse(actualNewInstanceResult.isLoggedIn());
    assertTrue(actualNewInstanceResult.isInRoom());
    assertFalse(actualNewInstanceResult.isActivated());
    assertFalse(actualNewInstanceResult.getSession().isPresent());
    assertEquals("Name", actualNewInstanceResult.getName());
    assertEquals(0L, actualNewInstanceResult.getLastLoggedInTime());
  }
}

