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

import java.util.Map;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.network.Network;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.server.Server;
import java.util.ArrayList;

/** Read-only views. Only an explicit pressure query scans the session snapshot. */
public final class ServerInspection {
  private static final int MAX_ROWS = 50;
  private final Server server;
  private final Network network;

  public ServerInspection(Server server, Network network) {
    this.server = server;
    this.network = network;
  }

  public OperationalHistory history() {
    return network.getSessionManager().getOperationalHistory();
  }

  public InspectionResult query(String input) {
    String[] words = input.strip().split("\\s+");
    var rows = new Rows();
    var sessions = network.getSessionManager();
    if (words.length == 2 && (words[0].equals("session") || words[0].equals("room"))) {
      long id;
      try {
        id = Long.parseLong(words[1]);
      } catch (NumberFormatException invalid) {
        return InspectionResult.message("Expected a numeric ID");
      }
      if (words[0].equals("room")) {
        var room = server.getRoomManager().getRoomById(id);
        if (room == null) {
          return InspectionResult.message("Room no longer exists");
        }
        rows.add("Room ID", room.getId());
        rows.add("Name", room.getName());
        rows.add("Owner", room.getOwner().map(Player::getIdentity).orElse(""));
        rows.add("Players", room.getSnapshotPlayerCount());
        rows.add("Capacity", room.getCapacity());
        room.getSnapshotPlayersList().stream().limit(MAX_ROWS)
            .forEach(player -> rows.add("Member", player.getIdentity()));
      } else {
        var session = sessions.getSessionById(id);
        if (session == null) {
          return InspectionResult.message("Session no longer exists");
        }
        rows.add("Session ID", id);
        var name = session.getName();
        var player = name == null ? null : server.getPlayerManager().getPlayerByIdentity(name);
        if (player != null && player.getSession().orElse(null) == session) {
          rows.add("Player", name);
          rows.add("Room ID", player.getCurrentRoom().map(Room::getId).orElse(null));
        }
        rows.add("Transport", session.getTransportType());
        rows.add("UDP attached", session.containsUdp());
        if (session.containsUdp()) {
          rows.add("Last UDP read age (ms)", "unavailable");
        }
        rows.add("Last read age (ms)", age(session.getLastReadTime()));
        if (session.isTcp()) {
          rows.add("Last reliable TCP read age (ms)", age(session.getLastReliableReadTime()));
        }
        rows.add("Last write age (ms)", age(session.getLastWriteTime()));
        var queue = session.fetchOutboundQueue();
        if (queue != null) {
          var stats = queue.statistics();
          rows.add("Outbound depth", stats.depth());
          rows.add("Outbound capacity", stats.capacity());
          rows.add("High-water mark", stats.highWater());
          rows.add("Full drops", stats.fullDrops());
          rows.add("Policy drops", stats.policyDrops());
        }
      }
    } else if (input.equals("stats") || input.equals("server stats")) {
      rows.add("Uptime (ms)", server.getUptime());
      rows.add("Players", server.getPlayerManager().getSnapshotPlayerCount());
      rows.add("Rooms", server.getRoomManager().getSnapshotRoomCount());
      rows.add("Sessions", sessions.getSnapshotSessionCount());
      rows.add("Bytes read", network.getNetworkReaderStatistic().getReadBytes());
      var writer = network.getNetworkWriterStatistic();
      rows.add("Bytes written", writer.getWrittenBytes());
      rows.add("Full drops", writer.getWrittenDroppedPacketsByFull());
      rows.add("Policy drops", writer.getWrittenDroppedPacketsByPolicy());
    } else if (input.equals("network pressure")) {
      int underPressure = 0;
      for (Session session : sessions.getSnapshotSessionsList()) {
        var queue = session.fetchOutboundQueue();
        if (queue != null) {
          var stats = queue.statistics();
          if (stats.capacity() > 0 && stats.depth() * 100L >= stats.capacity() * 80L) {
            underPressure++;
            if (rows.size() < MAX_ROWS) {
              rows.add("Session " + session.getId(), stats.depth() + "/" + stats.capacity()
                  + " for " + stats.pressureAgeMillis() + " ms");
            }
          }
        }
      }
      rows.add("Sessions under pressure", underPressure);
    } else if (input.equals("limits")) {
      var commands = server.getClientCommandManager();
      if (commands != null) {
        commands.limitCounts().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .limit(MAX_ROWS).forEach(e -> rows.add(e.getKey().toString(), e.getValue()));
      }
    } else if (input.equals("history") || (words.length == 2 && words[0].equals("history"))) {
      Long sessionId;
      try {
        sessionId = words.length == 2 ? Long.valueOf(words[1]) : null;
      } catch (NumberFormatException invalid) {
        return InspectionResult.message("Expected a numeric session ID");
      }
      var history = sessions.getOperationalHistory();
      if (history == null) {
        return InspectionResult.message("History is disabled");
      }
      rows.add("Dropped exports", history.droppedExports());
      history.snapshot().reversed().stream()
          .filter(e -> sessionId == null || sessionId == e.sessionId()).limit(MAX_ROWS)
          .forEach(e -> rows.add(e.timestamp() + " / " + e.sessionId(),
              e.transport() + " " + e.kind() + " " + e.reason()));
    } else {
      return InspectionResult.message("Commands: stats, session <id>, room <id>, network pressure, limits, history [session ID]");
    }
    return rows.result();
  }

  private static String age(long time) {
    return time <= 0 ? "unavailable" : Long.toString(Math.max(0, System.currentTimeMillis() - time));
  }

  private static final class Rows {
    private final ArrayList<String> labels = new ArrayList<>();
    private final ArrayList<String> values = new ArrayList<>();
    void add(String key, Object value) {
      labels.add(key);
      String text = value == null ? "unavailable" : value.toString();
      values.add(text.length() > 240 ? text.substring(0, 240) : text);
    }
    int size() {
      return labels.size();
    }
    InspectionResult result() {
      return new InspectionResult(labels, values);
    }
  }
}
