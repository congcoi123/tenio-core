package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import org.junit.jupiter.api.Test;

class RoomImplTest {
  @Test
  void testNewInstance() {
    Room actualNewInstanceResult = RoomImpl.newInstance();
    assertFalse(actualNewInstanceResult.isActivated());
    assertEquals(0, actualNewInstanceResult.getSpectatorCount());
    assertEquals(RoomRemoveMode.DEFAULT, actualNewInstanceResult.getRoomRemoveMode());
    assertNull(actualNewInstanceResult.getPlayerManager());
    assertEquals(0, actualNewInstanceResult.getPlayerCount());
    assertNull(actualNewInstanceResult.getOwner());
    assertEquals(0, actualNewInstanceResult.getMaxSpectators());
    assertEquals(0, actualNewInstanceResult.getMaxPlayers());
  }
}

