/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

package com.tenio.core.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.network.Network;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.server.Server;
import org.junit.jupiter.api.Test;

class ServerInspectionTest {
  @Test
  void pollingStatsUsesCountersWithoutWalkingEntities() {
    var server = mock(Server.class);
    var network = mock(Network.class);
    var sessions = mock(SessionManager.class);
    var rooms = mock(RoomManager.class);
    var players = mock(PlayerManager.class);
    when(server.getPlayerManager()).thenReturn(players);
    when(server.getRoomManager()).thenReturn(rooms);
    when(network.getSessionManager()).thenReturn(sessions);
    when(network.getNetworkReaderStatistic()).thenReturn(NetworkReaderStatistic.newInstance());
    when(network.getNetworkWriterStatistic()).thenReturn(NetworkWriterStatistic.newInstance());
    when(sessions.getSnapshotSessionCount()).thenReturn(23);

    var result = new ServerInspection(server, network).query("stats");
    assertEquals("23", result.values().get(result.labels().indexOf("Sessions")));
    verify(sessions, never()).getSnapshotSessionsList();
    verify(rooms, never()).getSnapshotRoomsList();
    verify(players, never()).getSnapshotPlayersList();
  }

  @Test
  void missingSessionAndUnknownCommandsAreReadOnly() {
    var server = mock(Server.class);
    var network = mock(Network.class);
    var sessions = mock(SessionManager.class);
    when(network.getSessionManager()).thenReturn(sessions);
    var inspection = new ServerInspection(server, network);

    assertEquals(InspectionResult.message("Session no longer exists"), inspection.query("session 7"));
    assertTrue(inspection.query("server stop").values().getFirst().startsWith("Commands"));
    verifyNoInteractions(server);
  }
}
