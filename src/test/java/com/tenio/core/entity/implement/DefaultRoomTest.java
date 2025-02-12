package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import org.junit.jupiter.api.Test;

class DefaultRoomTest {
  @Test
  void testNewInstance() {
    Room actualNewInstanceResult = DefaultRoom.newInstance();
    assertFalse(actualNewInstanceResult.isActivated());
    assertEquals(0, actualNewInstanceResult.getSpectatorCount());
    assertEquals(RoomRemoveMode.WHEN_EMPTY, actualNewInstanceResult.getRoomRemoveMode());
    assertEquals(0, actualNewInstanceResult.getParticipantCount());
    assertTrue(actualNewInstanceResult.getOwner().isEmpty());
    assertEquals(0, actualNewInstanceResult.getMaxSpectators());
    assertEquals(0, actualNewInstanceResult.getMaxParticipants());
  }
}

