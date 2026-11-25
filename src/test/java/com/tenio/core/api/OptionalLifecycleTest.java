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

package com.tenio.core.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.api.implement.ServerApiImpl;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.discovery.RoomDirectory;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.entity.replication.StateReplication;
import com.tenio.core.server.Server;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OptionalLifecycleTest {
  @Test
  void logoutOfAnOldPlayerObjectDoesNotDestroyNewPlayersStateOrSubscriptions() {
    var server = mock(Server.class);
    var players = mock(PlayerManager.class);
    var rooms = mock(RoomManager.class);
    when(server.getPlayerManager()).thenReturn(players);
    when(server.getRoomManager()).thenReturn(rooms);
    var old = mock(Player.class);
    var current = mock(Player.class);
    when(old.getIdentity()).thenReturn("alice");
    when(old.getSession()).thenReturn(Optional.empty());
    when(players.getPlayerByIdentity("alice")).thenReturn(current);
    try (var replication = new StateReplication(10, 1024, 10);
        var directory = new RoomDirectory(Set.of(), 10, 10)) {
      when(server.getStateReplication()).thenReturn(replication);
      when(rooms.getDirectory()).thenReturn(directory);
      var state =
          replication.createPlayer(
              "alice",
              Map.of(
                  "ready",
                  new ReplicatedState.Definition(
                      ReplicatedState.Type.BOOLEAN, ReplicatedState.Visibility.ROOM, false, 1)));
      var subscription = directory.subscribe("alice", "group");
      ServerApiImpl.newInstance(server)
          .logout(
              old, ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
      assertSame(state, replication.player("alice"));
      var room = mock(Room.class);
      when(room.getName()).thenReturn("room");
      when(room.getId()).thenReturn(1L);
      directory.publish(room, "group", "game", "waiting", Map.of());
      assertNotNull(subscription.poll());
    }
  }
}
