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

package com.tenio.core.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.bootstrap.BootstrapHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.entity.manager.ChannelManager;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.server.Server;
import com.tenio.core.server.ServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {

  private Server server;

  @BeforeEach
  void setUp() {
    server = ServerImpl.getInstance();
  }

  @Test
  void testSingleton() {
    Server s1 = ServerImpl.getInstance();
    Server s2 = ServerImpl.getInstance();
    assertSame(s1, s2);
  }

  // @Test
  // void testGetters() {
  //   assertNotNull(server.getApi());
  //   assertNotNull(server.getClientCommandManager());
  //   assertNotNull(server.getEventManager());
  //   assertNotNull(server.getPlayerManager());
  //   assertNotNull(server.getRoomManager());
  //   assertNotNull(server.getChannelManager());
  //   assertNotNull(server.getDatagramChannelManager());
  //   assertNotNull(server.getConfiguration());
  //   assertNotNull(server.getDataType());
  //   assertTrue(server.getStartedTime() >= 0);
  //   assertTrue(server.getUptime() >= 0);
  // }

  // @Test
  // void testWrite() {
  //   Response response = mock(Response.class);
  //   assertDoesNotThrow(() -> server.write(response, true));
  //   assertDoesNotThrow(() -> server.write(response, false));
  // }
}
