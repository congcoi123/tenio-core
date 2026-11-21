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

import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.network.Network;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.server.Server;
import java.util.List;
import java.util.Optional;
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
    var reader = NetworkReaderStatistic.newInstance();
    reader.updateReadBytes(120302);
    when(network.getNetworkReaderStatistic()).thenReturn(reader);
    when(server.getUptime()).thenReturn(120000L);
    when(rooms.getSnapshotRoomCount()).thenReturn(1203);
    when(network.getNetworkWriterStatistic()).thenReturn(NetworkWriterStatistic.newInstance());
    when(sessions.getSnapshotSessionCount()).thenReturn(23);

    var result = new ServerInspection(server, network).query("stats");
    assertEquals("23", result.values().get(result.labels().indexOf("Sessions")));
    assertEquals("1,203", result.values().get(result.labels().indexOf("Rooms")));
    assertEquals("120,302", result.values().get(result.labels().indexOf("Bytes read")));
    assertEquals("2 minutes", result.values().get(result.labels().indexOf("Uptime")));
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
    assertTrue(inspection.query("history 7").values().getFirst().contains("restart TenIO"));
    verifyNoInteractions(server);
  }
  @Test
  void pinnedCommandsHaveHelpAndRunThroughInspection() {
    var server = mock(Server.class);
    var network = mock(Network.class);
    var sessions = mock(SessionManager.class);
    var rooms = mock(RoomManager.class);
    var session = mock(Session.class);
    var queue = mock(OutboundQueue.class);
    var room = mock(Room.class);
    var commands = new ClientCommandManager();
    try (var history = new OperationalHistory(4)) {
      when(network.getSessionManager()).thenReturn(sessions);
      when(server.getRoomManager()).thenReturn(rooms);
      when(server.getClientCommandManager()).thenReturn(commands);
      when(rooms.getRoomById(42)).thenReturn(room);
      when(room.getOwner()).thenReturn(Optional.empty());
      when(room.getSnapshotPlayersList()).thenReturn(List.of());
      when(sessions.getSessionById(7)).thenReturn(session);
      when(sessions.getSnapshotSessionsList()).thenReturn(List.of(session));
      when(session.getId()).thenReturn(7L);
      when(session.fetchOutboundQueue()).thenReturn(queue);
      when(queue.statistics()).thenReturn(new OutboundQueue.Statistics(8, 10, 8, 0, 0, 12));
      when(sessions.getOperationalHistory()).thenReturn(history);
      commands.getLimits().configure((short) 4, 1, 1);
      history.record(7, "TCP", OperationalHistory.Kind.DISCONNECT, "idle");
      history.record(8, "TCP", OperationalHistory.Kind.RECONNECT, "resumed");

      var inspection = new ServerInspection(server, network);
      assertEquals("help | stats | room <id> | session <id> | network pressure | limits | history [id] | clear",
          AdminCommands.shortcuts());
      assertEquals(List.of("stats", "room <id>", "session <id>", "network pressure", "limits",
          "history [id]", "clear"), inspection.query("help").labels());
      assertTrue(inspection.query("ROOM  42").labels().contains("Room ID"));
      assertTrue(inspection.query("session 7").labels().contains("Outbound depth"));
      assertTrue(inspection.query("network pressure").labels().contains("Session 7"));
      assertTrue(inspection.query("limits").labels().contains("4"));
      assertEquals(3, inspection.query("history").labels().size());
      assertEquals(2, inspection.query("history 7").labels().size());
    }
  }

  @Test
  void uptimeUsesReadableUnits() {
    assertEquals("just now", ServerInspection.formatUptime(1));
    assertEquals("1 minute", ServerInspection.formatUptime(60000));
    assertEquals("2 hours", ServerInspection.formatUptime(7200000));
    assertEquals("1 day", ServerInspection.formatUptime(86400000));
  }
}
