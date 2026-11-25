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

package com.tenio.core.scheduler.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.admin.PerformanceHistory;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.network.Network;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.server.Server;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerformanceJobTest {
  @TempDir Path directory;

  @Test
  void samplesGaugesAndActualRateDeltasWithoutScanningEntities() throws Exception {
    var server = mock(Server.class);
    var network = mock(Network.class);
    var players = mock(PlayerManager.class);
    var rooms = mock(RoomManager.class);
    var sessions = mock(SessionManager.class);
    var reader = mock(NetworkReaderStatistic.class);
    var writer = mock(NetworkWriterStatistic.class);
    when(server.getPlayerManager()).thenReturn(players);
    when(server.getRoomManager()).thenReturn(rooms);
    when(network.getSessionManager()).thenReturn(sessions);
    when(server.getClientCommandManager()).thenReturn(new ClientCommandManager());
    when(network.getNetworkReaderStatistic()).thenReturn(reader);
    when(network.getNetworkWriterStatistic()).thenReturn(writer);
    when(players.getSnapshotPlayerCount()).thenReturn(2);
    when(rooms.getSnapshotRoomCount()).thenReturn(1);
    when(sessions.getSnapshotSessionCount()).thenReturn(3);
    when(reader.getReadBytes()).thenReturn(100L, 200L);
    when(writer.getWrittenBytes()).thenReturn(200L, 500L);
    try (var history = new PerformanceHistory(directory, true)) {
      var job = new PerformanceJob();
      job.configure(server, network, history);
      job.sample();
      job.sample();
      var samples = history.recent();
      assertEquals(2, samples.size());
      assertEquals(2, samples.getFirst().players());
      assertEquals(1, samples.getFirst().rooms());
      assertEquals(3, samples.getFirst().sessions());
      assertEquals(0, samples.getFirst().readBytesPerSecond());
      assertTrue(samples.getLast().readBytesPerSecond() > 0);
      assertTrue(samples.getLast().writtenBytesPerSecond() > 0);
      assertTrue(samples.getLast().heapBytes() > 0);
      verify(sessions, never()).getSnapshotSessionsList();
      verify(rooms, never()).getSnapshotRoomsList();
    }
  }
}
