/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tenio.core.configuration.Configuration;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.entity.implement.DefaultRoom;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.entity.manager.implement.PlayerManagerImpl;
import com.tenio.core.entity.manager.implement.RoomManagerImpl;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.AddedDuplicatedRoomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Unit Test Cases For Player And Room")
class PlayerAndRoomTest {

  private EventManager eventManager;

  private PlayerManager playerManager;
  private RoomManager roomManager;

  private String testPlayerName;
  private String testRoomId;

  @BeforeEach
  public void initialize() throws Exception {
    var configuration = new Configuration();
    configuration.load("configuration.example.xml");

    eventManager = EventManager.newInstance();
    playerManager = PlayerManagerImpl.newInstance(eventManager);
    roomManager = RoomManagerImpl.newInstance(eventManager);
    testPlayerName = "kong";
    testRoomId = "test-room-id";
  }

  @AfterEach
  public void tearDown() {
    eventManager.clear();
    playerManager.clear();
  }

  @Test
  @DisplayName("Add new player should return success")
  public void addNewPlayerShouldReturnSuccess() {
    Player player = DefaultPlayer.newInstance(testPlayerName);
    playerManager.addPlayer(player);
    Player result = playerManager.getPlayerByIdentity(testPlayerName);

    assertEquals(player, result);
  }

  @Test
  @DisplayName("Add duplicated player should cause exception")
  public void addDuplicatedPlayerShouldCauseException() {
    assertThrows(AddedDuplicatedPlayerException.class, () -> {
      Player player = DefaultPlayer.newInstance(testPlayerName);
      playerManager.addPlayer(player);
      playerManager.addPlayer(player);
    });
  }

  @Test
  @DisplayName("Check contain player should return success")
  public void checkContainPlayerShouldReturnSuccess() {
    var player = DefaultPlayer.newInstance(testPlayerName);
    playerManager.addPlayer(player);

    assertTrue(playerManager.containsPlayerIdentity(testPlayerName));
  }

  @Test
  @DisplayName("Get player count should return correct value")
  public void getPlayerCountShouldReturnCorrectValue() {
    for (int i = 0; i < 10; i++) {
      Player player = DefaultPlayer.newInstance(testPlayerName + i);
      playerManager.addPlayer(player);
    }

    assertEquals(10, playerManager.getPlayerCount());
  }

  @Test
  @DisplayName("Remove player should return success")
  public void removePlayerShouldReturnSuccess() {
    Player player = DefaultPlayer.newInstance(testPlayerName);
    playerManager.addPlayer(player);
    playerManager.removePlayerByIdentity(testPlayerName);

    assertNull(playerManager.getPlayerByIdentity(testPlayerName));
  }

  @Test
  @DisplayName("Get player by identity should return correct player")
  public void getPlayerByIdentityShouldReturnCorrectPlayer() {
    Player player = DefaultPlayer.newInstance(testPlayerName);
    playerManager.addPlayer(player);

    assertEquals(player, playerManager.getPlayerByIdentity(testPlayerName));
  }

  @Test
  @DisplayName("Create room should return success")
  public void createRoomShouldReturnSuccess() {
    Room room = DefaultRoom.newInstance();
    roomManager.addRoom(room);

    assertEquals(room, roomManager.getRoomById(room.getId()));
  }

  @Test
  @DisplayName("Create duplicated room should cause exception")
  public void createDuplicatedRoomShouldCauseException() {
    assertThrows(AddedDuplicatedRoomException.class, () -> {
      var room = DefaultRoom.newInstance();
      room.configurePlayerManager(Mockito.mock(PlayerManager.class));
      roomManager.addRoom(room);
      roomManager.addRoom(room);
    });
  }
}
