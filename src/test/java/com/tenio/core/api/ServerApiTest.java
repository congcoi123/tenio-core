/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

package com.tenio.core.api;

import com.tenio.core.api.implement.ServerApiImpl;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerLeaveRoomMode;
import com.tenio.core.entity.define.result.PlayerLeftRoomResult;
import com.tenio.core.entity.define.result.PlayerLoggedInResult;
import com.tenio.core.entity.implement.PlayerImpl;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.RemovedNonExistentPlayerFromRoomException;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.implement.SessionImpl;
import com.tenio.core.server.Server;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("All unit test cases for the Server Api")
@ExtendWith(MockitoExtension.class)
class ServerApiTest {

  @Mock
  Server server;

  @Mock
  PlayerManager playerManager;

  @Mock
  RoomManager roomManager;

  @Mock
  EventManager eventManager;

  @Mock
  Optional<Room> optionalRoom;

  @Mock
  Optional<Session> optionalSession;

  ServerApi serverApi;

  @BeforeEach
  void initialization() {
    serverApi = ServerApiImpl.newInstance(server);
  }

  @Test
  @DisplayName("It should create a new instance")
  void itShouldCreateNewInstance() {
    ServerApiImpl.newInstance(Mockito.mock(Server.class));
  }

  @Test
  @DisplayName("When a player login without session, and it has no exception")
  void playerLoginWithoutSessionShouldHaveNoException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginPlayer = PlayerImpl.newInstance(loginName);
    Mockito.when(playerManager.createPlayer(loginName)).thenReturn(loginPlayer);
    serverApi.login(loginName);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, loginPlayer,
            PlayerLoggedInResult.SUCCESS);
  }

  @Test
  @DisplayName("When a player login without session, and it has duplicated exception")
  void playerLoginWithoutSessionShouldHaveDuplicatedException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginPlayer = PlayerImpl.newInstance(loginName);
    Mockito.doThrow(new AddedDuplicatedPlayerException(loginPlayer, Mockito.mock(Room.class)))
        .when(playerManager).createPlayer(loginName);
    serverApi.login(loginName);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, null,
            PlayerLoggedInResult.DUPLICATED_PLAYER);
  }

  @Test
  @DisplayName("When a player login with a session, it should have no exception")
  void playerLoginWithSessionShouldHaveNoException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginPlayer = PlayerImpl.newInstance(loginName);
    var loginSession = SessionImpl.newInstance();
    Mockito.when(playerManager.createPlayerWithSession(loginName, loginSession))
        .thenReturn(loginPlayer);
    serverApi.login(loginName, loginSession);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, loginPlayer,
            PlayerLoggedInResult.SUCCESS);
  }

  @Test
  @DisplayName("When a player login with an unavailable session, it should have null pointer " +
      "exception")
  void playerLoginWithSessionShouldHaveNullPointerException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginSession = SessionImpl.newInstance();
    Mockito.doThrow(NullPointerException.class)
        .when(playerManager).createPlayerWithSession(loginName, loginSession);
    serverApi.login(loginName, loginSession);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, null,
            PlayerLoggedInResult.SESSION_NOT_FOUND);
  }

  @Test
  @DisplayName("When a player login with a session, and it has duplicated exception")
  void playerLoginWithSessionShouldHaveDuplicatedException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginPlayer = PlayerImpl.newInstance(loginName);
    var loginSession = SessionImpl.newInstance();
    Mockito.doThrow(new AddedDuplicatedPlayerException(loginPlayer, Mockito.mock(Room.class)))
        .when(playerManager).createPlayerWithSession(loginName, loginSession);
    serverApi.login(loginName, loginSession);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, null,
            PlayerLoggedInResult.DUPLICATED_PLAYER);
  }

  @Test
  @DisplayName("When it tries to logout a null player, it should not do any further action")
  void itLogoutNullPlayerShouldDoNothingFurther() {
    serverApi.logout(null);
    Mockito.verifyNoMoreInteractions(eventManager, playerManager);
  }

  @Test
  @DisplayName("When it tries to logout a player in a room, the player should leave the room first")
  void itLogoutPlayerInRoomShouldLeaveRoomFirst() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var player = Mockito.mock(Player.class);
    var room = Mockito.mock(Room.class);
    Mockito.when(optionalRoom.get()).thenReturn(room);
    optionalRoom.get().addPlayer(player);
    Mockito.when(player.isInRoom()).thenReturn(true);
    Mockito.when(player.getCurrentRoom()).thenReturn(optionalRoom);
    serverApi.logout(player);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_BEFORE_LEAVE_ROOM, player, optionalRoom,
            PlayerLeaveRoomMode.LOG_OUT);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_AFTER_LEFT_ROOM, player, optionalRoom,
            PlayerLeftRoomResult.SUCCESS);
  }

  @Test
  @DisplayName("When it tries to logout a player, and remove him from a nonexistent room, an " +
      "exception RemovedNonExistentPlayer should be thrown")
  void itLogoutPlayerNotInRoomShouldHaveRemovedNonExistentPlayerException() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var player = Mockito.mock(Player.class);
    var room = Mockito.mock(Room.class);
    Mockito.when(optionalRoom.get()).thenReturn(room);
    optionalRoom.get().addPlayer(player);
    Mockito.when(player.isInRoom()).thenReturn(true);
    Mockito.when(player.getCurrentRoom()).thenReturn(optionalRoom);
    Mockito.doThrow(RemovedNonExistentPlayerFromRoomException.class)
        .when(room).removePlayer(player);
    serverApi.logout(player);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_BEFORE_LEAVE_ROOM, player, optionalRoom,
            PlayerLeaveRoomMode.LOG_OUT);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_AFTER_LEFT_ROOM, player, optionalRoom,
            PlayerLeftRoomResult.PLAYER_ALREADY_LEFT_ROOM);
  }

  @Test
  @DisplayName("When it tries to logout a player which has session, the session should be closed")
  void itLogoutPlayerHasSessionShouldCloseSession() throws IOException {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var player = Mockito.mock(Player.class);
    var session = Mockito.mock(Session.class);
    Mockito.when(optionalSession.get()).thenReturn(session);
    Mockito.when(player.getSession()).thenReturn(optionalSession);
    Mockito.when(player.containsSession()).thenReturn(true);
    serverApi.logout(player);
    Mockito.verify(session, Mockito.times(1))
        .close(ConnectionDisconnectMode.DEFAULT, PlayerDisconnectMode.DEFAULT);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.DISCONNECT_PLAYER, player, PlayerDisconnectMode.DEFAULT);
  }

  @Test
  @DisplayName("When it tries to logout a player which has session, and the closed session has IO" +
      " exception")
  void itLogoutPlayerHasSessionShouldHaveClosedSessionIoException() throws IOException {
    var player = Mockito.mock(Player.class);
    var session = Mockito.mock(Session.class);
    Mockito.when(optionalSession.get()).thenReturn(session);
    Mockito.when(player.getSession()).thenReturn(optionalSession);
    Mockito.when(player.containsSession()).thenReturn(true);
    Mockito.doThrow(IOException.class)
        .when(session).close(ConnectionDisconnectMode.DEFAULT, PlayerDisconnectMode.DEFAULT);
    serverApi.logout(player);
    Mockito.verifyNoMoreInteractions(eventManager, playerManager);
  }
}
