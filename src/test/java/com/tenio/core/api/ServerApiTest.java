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
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.result.PlayerLoggedInResult;
import com.tenio.core.entity.implement.PlayerImpl;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.server.Server;
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
  EventManager eventManager;

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
  @DisplayName("When a player login without session, it should have expected behaviors")
  void playerLoginWithoutSessionShouldHasExpectedBehaviours() {
    Mockito.when(server.getPlayerManager()).thenReturn(playerManager);
    Mockito.when(server.getEventManager()).thenReturn(eventManager);

    var loginName = "test";
    var loginPlayer = PlayerImpl.newInstance(loginName);
    Mockito.when(playerManager.createPlayer(loginName)).thenReturn(loginPlayer);
    serverApi.login(loginName);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, loginPlayer,
            PlayerLoggedInResult.SUCCESS);

    Mockito.doThrow(new AddedDuplicatedPlayerException(loginPlayer, Mockito.mock(Room.class)))
        .when(playerManager).createPlayer(loginName);
    serverApi.login(loginName);
    Mockito.verify(eventManager, Mockito.times(1))
        .emit(ServerEvent.PLAYER_LOGGEDIN_RESULT, null,
            PlayerLoggedInResult.DUPLICATED_PLAYER);
  }
}
